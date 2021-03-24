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

  case class RateLimiterConfig(minInterval: Duration)

  type RateLimiter = Has[RateLimiter.Service]

  object RateLimiter {

    // WIP
    trait Service {
      def delay: ZIO[Any,Nothing,Unit]
    }

    val live:  ZLayer[Clock with Console with Has[RateLimiterConfig], Nothing, Has[Service]] =
      ZLayer.fromServicesM[Clock.Service,Console.Service,RateLimiterConfig,
        Clock with Console with Has[RateLimiterConfig],
        Nothing,
        Service] {
        (clock: Clock.Service,console: Console.Service,config: RateLimiterConfig) =>
          val minInterval = config.minInterval.toNanos().toLong
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
                        //console.putStrLn(s"Waiting ${wait/1000000}ms") *>
                        clock.sleep(Duration.fromNanos(wait))
                      else
                        //console.putStrLn("No wait") *>
                        ZIO.succeed(())
                ) yield ())
              }
          }
      }

    def delay  = ZIO.accessM[RateLimiter](_.get.delay)
  }
}