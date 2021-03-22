package org.justinhj

import zio.ZIO
import zio.clock.Clock
import zio.duration._
import zio.Queue
import zio.Schedule
import zio.Has
import zio.ZLayer

package object ratelimiter {

  // Based on the rate limiter in this post with some tweaks and ZLayer-ized
  // https://medium.com/wix-engineering/building-a-super-easy-rate-limiter-with-zio-88f1ccb49776

  // TODO write tests
  // TODO fix bug with no delay between first and second

  type RateLimiter = Has[RateLimiter.Service]

  object RateLimiter {

    trait Service {
      def delay: ZIO[Any,Nothing,Unit]
    }

    val live: ZLayer[Clock, Nothing, Has[Service]] = {
      ZLayer.fromEffect(
        makeQueue(2.second).map {
          queue =>
            new Service {
              def delay =
                for (
                  _ <- ZIO(println(s"queue pre offered at ${System.currentTimeMillis()}}")).orElse(ZIO.succeed(()));
                  _ <- queue.offer(());
                  _ <- ZIO(println(s"queue offered at ${System.currentTimeMillis()}}")).orElse(ZIO.succeed(()))
                ) yield ()
              }
          }
        )
    }

    def delay: ZIO[RateLimiter with Clock, Nothing, Unit] = ZIO.accessM[RateLimiter](_.get.delay)

    def makeQueue(interval: Duration): ZIO[Clock, Nothing, Queue[Unit]] = {
      require(!interval.isNegative)
      for {
        queue <- Queue.bounded[Unit](1)
        _ <- (ZIO.sleep(interval) *> queue.take.repeat(Schedule.fixed(interval))).fork
        _ <- ZIO(println(s"made queue at ${System.currentTimeMillis()}}")).orElse(ZIO.succeed(()))
      } yield queue
    }
  }
}