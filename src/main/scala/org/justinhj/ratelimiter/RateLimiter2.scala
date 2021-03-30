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
          val minInterval = config.minInterval.toNanos()
          currentTime(TimeUnit.NANOSECONDS).
            flatMap(t => Ref.make(t - minInterval)).map {
              timeRef =>
                new Service {
                  def delay: ZIO[Any,Nothing,Unit] = (for (
                    now <- clock.currentTime(TimeUnit.NANOSECONDS);
                    nextTime <- timeRef.get; // the ref holds the next time you can go ahead
                    wait = if(nextTime - now > 0) nextTime - now else 0;
                    _ <- timeRef.set(now + minInterval + wait);
                    _ <- ZIO.succeed(println(s"nextTime ${nextTime / 1000000}\nwait ${wait / 1000000}\nnow ${now / 1000000}\n"));
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