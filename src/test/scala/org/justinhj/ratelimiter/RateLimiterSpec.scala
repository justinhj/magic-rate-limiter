package org.justinhj.ratelimiter2

import zio.ZIO
import zio.ZLayer
import zio.clock._
import zio.test.environment.TestClock._
import zio.duration._
import zio.magic._
import zio.test.Assertion._
import zio.test._
import zio.test.environment._
import java.util.concurrent.TimeUnit
//import zio.Schedule
import zio.Queue
import zio.console._

// Tests for the Ref based rate limiter

object RateLimiterSpec extends DefaultRunnableSpec {

  // Set up a rate limit config of one second per interval...
  private val configLayer = ZLayer.succeed(RateLimiter.Config(1000 millis))

  private val testLayer = (TestClock.any ++ configLayer) >>> RateLimiter.live

  def spec: ZSpec[Environment, Failure] =
    suite("RateLimiterSpec")(
      /**
        * To prove that the first request is not limited we need a fiber that
        * will write to the queue in one second, and another that will be rate
        * limited but write immediately. We should see that the first effect
        * is written first...
        */
      testM("Rate limiter does not limit first request") {
        (for (
          outputs <- Queue.bounded[Int](10);
          _ <- (ZIO.sleep(500 millis) *> outputs.offer(2)).fork; // Should happen halfway through first limit period
          _ <- (RateLimiter.delay *> outputs.offer(1)).fork; // Should happen first
          _ <- adjust(1000 millis);
          contents <- outputs.takeAll
        ) yield assert(contents)(equalTo(List(1,2)))).provideSomeMagicLayer(testLayer)
      }
    )

}
