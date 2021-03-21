package org.justinhj

import zio.ZIO
import zio.clock.Clock
import zio.duration._
import zio.Queue
import zio.Schedule
import zio.Has
import zio.ZLayer
import zio.clock._
import zio.Ref

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

    private def startDraining(queue: Queue[Unit], period: Duration) = (for (
        _ <- queue.take;
        _ <- ZIO(println(s"queue take at ${System.currentTimeMillis()}}")).orElse(ZIO.succeed(()))
      ) yield ()).repeat(Schedule.fixed(period)).fork.map(_ => ())

    val live: ZLayer[Clock, Nothing, Has[Service]] = {
      ZLayer.fromEffect(
        Ref.make(true).flatMap {
          isFirstRef =>
            makeQueue(0.25).map {
              queue =>
                new Service {
                  def delay =
                    (for (
                      isFirst <- isFirstRef.get;
                      _ <- if(isFirst)
                          startDraining(queue, Duration.fromMillis(3000)).provideLayer(Clock.live) *>
                          isFirstRef.set(false)
                        else
                          ZIO.succeed(());
                      _ <- ZIO(println(s"queue pre offered at ${System.currentTimeMillis()}}")).orElse(ZIO.succeed(()));
                      _ <- queue.offer(());
                      _ <- ZIO(println(s"queue offered at ${System.currentTimeMillis()}}")).orElse(ZIO.succeed(()))
                    ) yield ())
              }
          }
        })
    }

    def delay: ZIO[RateLimiter with Clock, Nothing, Unit] = ZIO.accessM[RateLimiter](_.get.delay)

    def makeQueue(
        perSecond: Double,
        buffer: Int = 1
    ): ZIO[Clock, Nothing, Queue[Unit]] = {
      require(perSecond > 0 && buffer > 0)
      //val period: Duration = periodFrom(perSecond)
      for {
        queue <- Queue.bounded[Unit](buffer);
        _ <- ZIO(println(s"made queue at ${System.currentTimeMillis()}}")).orElse(ZIO.succeed(()))
      } yield queue
    }

    private def periodFrom(perSecond: Double) =
      (1.second.toNanos.toDouble / perSecond).toInt.nanos
  }
}