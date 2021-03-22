package org.justinhj

import zio.ZIO
import zio.clock._
import zio.duration._
import zio.Has
import zio.ZLayer
import zio.Ref
import java.util.concurrent.TimeUnit
import zio.console._

package object ratelimiter2 {

  type RateLimiter = Has[RateLimiter.Service]

  object RateLimiter {

    trait Service {
      def delay: ZIO[Any,Nothing,Unit]
    }

    // TODO can you use duration arithmetic?
    val live: ZLayer[Clock with Console, Nothing, Has[Service]] = {
      val minInterval: Long = 1.seconds.toNanos.toLong
      ZLayer.fromEffect(
        ZIO.access[Console](_.get).flatMap {
          console =>
            ZIO.access[Clock](_.get).flatMap {
              clock =>
                currentTime(TimeUnit.NANOSECONDS).flatMap(Ref.make).map {
                  timeRef =>
                    new Service {
                      def delay: ZIO[Any,Nothing,Unit] = (for (
                        now <- clock.currentTime(TimeUnit.NANOSECONDS);
                        prevTime <- timeRef.get;
                        _ <- timeRef.set(now);
                        elapsed = now - prevTime;
                        wait = minInterval - elapsed;
                        _ <- if(wait > 0)
                              console.putStrLn(s"Waiting ${wait/1000000}ms") *>
                              clock.sleep(Duration.fromNanos(wait))
                            else
                              console.putStrLn("No wait") *>
                              ZIO.succeed(())

                      ) yield ())
                    }
                }
              }
        })
      }

    def delay  = ZIO.accessM[RateLimiter](_.get.delay)
  }
}