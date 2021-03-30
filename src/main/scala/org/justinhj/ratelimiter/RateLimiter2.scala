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

    // Given a time to wait until and the current time
    // calculate how long you need to wait, if at all
    private def calcWait(until: Long, now: Long): Long = {
      val diff = until - now
      if(diff > 0) diff else 0
    }

    /**
      * Live implementation of the rate limiter. This guarantees no more than n
      * effects will be initiated in a period of n * minInterval durations.
      * Example if you set minInterval to 1 second, over 15 minutes you can do
      * a maximum of 900 requests.
      *
      * This works by using a ZIO Ref, a thread safe mutable variable we can thread
      * through the computation. Each time the delay function is called it checks if
      * the minimum interval has passed since the last call, and if not sleeps for that
      * amount of time. This means that any effects following the delay will also be
      * delayed so as not to break the rate limit.
      *
      * Note that the Ref that holds the time is got and set in the same atomic operation
      * using the modifiy method. This is important as other fibers could be accessing
      * the ref at the same time, so if you did the get and set as different steps then
      * the rate limit could be broken by that race condition.
      */

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
                    prevTime <- timeRef.modify{
                      current => {
                        val wait = calcWait(current,now)
                        (current, now + minInterval + wait)
                      }
                    };
                    wait = calcWait(prevTime,now);
                    _ <- if(wait > 0) clock.sleep(Duration.fromNanos(wait))
                        else ZIO.succeed(())
                  ) yield ())
                }
            }
      }

    def delay  = ZIO.accessM[RateLimiter](_.get.delay)
  }
}