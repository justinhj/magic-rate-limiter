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

  object RateLimiter {

    trait Service {
      def delay: ZIO[Any,Nothing,Unit]
    }

    // TODO can you use duration arithmetic?
    val live: ZLayer[Clock, Nothing, Has[Service]] = {
      val minInterval: Long = 3.seconds.toNanos.toLong

      ZLayer.fromEffect(
        ZIO.access[Clock](_.get).flatMap {
          clock =>
            currentTime(TimeUnit.NANOSECONDS).flatMap(Ref.make).map {
              timeRef =>
                new Service {
                  def delay: ZIO[Any,Nothing,Unit] = (for (
                    now <- currentTime(TimeUnit.NANOSECONDS);
                    prevTime <- timeRef.get;
                    elapsed = now - prevTime;
                    wait = minInterval - elapsed;
                    _ <- if(wait > 0)
                          ZIO.sleep(Duration.fromNanos(wait))
                        else
                          ZIO.succeed(())

                  ) yield ()).provideSomeLayer(ZLayer.fromService(clock))
                }
            }
          })
        }

      // need to wait 300
      // last time was 0
      // time now is 100
      // elapsed = now - then = 100 - 0 = 100 (should always be positive)
      // wait = 300 - elapsed = 200
      // time now is 310
      // elapsed is 310
      // wait = 300 - 310 = -'ve so no wait

    def delay  = ZIO.accessM[RateLimiter](_.get.delay)

  }
}