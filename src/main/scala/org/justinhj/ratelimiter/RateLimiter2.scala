package org.justinhj

import zio.ZIO
import zio.clock._
import zio.duration._
import zio.Has
import zio.ZLayer
import zio.Ref
import java.util.concurrent.TimeUnit

package object ratelimiter2 {

  type RateLimiter = Has[RateLimiter.Service]
  type RateLimiterConfig = Has[RateLimiter.Config]

  object RateLimiter {
    trait Service {
      def delay: ZIO[Any,Nothing,Unit]
    }

    case class Config(minInterval: Duration)

    val live:  ZLayer[Clock with RateLimiterConfig, Throwable, RateLimiter] =
      ZLayer.fromServicesM[Clock.Service,Config,
        Clock with RateLimiterConfig,
        Throwable,
        Service] {
        (clock: Clock.Service,config: Config) =>
          val minInterval = config.minInterval.toNanos().toLong
          currentTime(TimeUnit.NANOSECONDS).flatMap(Ref.make).map {
            timeRef =>
              new Service {
                def delay: ZIO[Any,Nothing,Unit] = (for (
                  now <- clock.currentTime(TimeUnit.NANOSECONDS);
                  _ <- ZIO.succeed(println(s"lol now $now"));
                  prevTime <- timeRef.get;
                  _ <- timeRef.set(now);
                  elapsed = now - prevTime;
                  wait = minInterval - elapsed;
                  _ <- if(wait > 0)
                        clock.sleep(Duration.fromNanos(wait))
                      else
                        ZIO.succeed(())
                ) yield ())
              }
          }
      }

    def delay  = ZIO.accessM[RateLimiter](_.get.delay)
  }
}