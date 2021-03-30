package org.justinhj.ratelimiter2

import zio.ZIO
import zio.ZLayer
import zio.test.environment.TestClock._
import zio.duration._
import zio.magic._
import zio.test.Assertion._
import zio.test._
import zio.test.environment._
import zio.Queue

// Tests for the Ref based rate limiter

object RateLimiterSpec extends DefaultRunnableSpec {

  // Set up a rate limit config of one second per interval...
  private val configLayer = ZLayer.succeed(RateLimiter.Config(1000 millis))

  private val testLayer = (TestClock.any ++ configLayer) >>> RateLimiter.live

  def spec: ZSpec[Environment, Failure] =
    suite("RateLimiterSpec")(
      /**
        * The strategy here is to sleep a fiber to points that I want to check
        * when certain events are happening, and write an identifying integer
        * to a ZQueue. Then events do the same; when they trigger they write to
        * the queue as well, and then I can check that the order of events is as
        * expected at the end.
        */
      testM("Rate limiter does not limit first request") {
        (for (
          outputs <- Queue.bounded[Int](10);
          _ <- (ZIO.sleep(500 millis) *> outputs.offer(2)).fork;
          _ <- (RateLimiter.delay *> outputs.offer(1)).fork;
          _ <- adjust(1000 millis);
          contents <- outputs.takeAll
        ) yield assert(contents)(equalTo(List(1,2)))).provideSomeMagicLayer(testLayer)
      },
      testM("Rate limiter handles two requests") {
        (for (
          outputs <- Queue.bounded[Int](10);
          _ <- (ZIO.sleep(1000 millis) *> outputs.offer(2)).fork;
          _ <- (ZIO.sleep(1200 millis) *> outputs.offer(3)).fork;
          _ <- adjust(100 millis);
          _ <- (RateLimiter.delay *> outputs.offer(0)).fork;
          _ <- adjust(100 millis);
          _ <- (RateLimiter.delay *> outputs.offer(1)).fork;
          _ <- adjust(2000 millis);
          contents <- outputs.takeAll
        ) yield assert(contents)(equalTo(List(0,2,1,3)))).provideSomeMagicLayer(testLayer)
      },
      testM("Rate limiter takes 10 seconds to do 10 requests") {
        (for (
          outputs <- Queue.bounded[Int](20);
          _ <- (ZIO.sleep(10000 millis) *> outputs.offer(11)).fork;
          _ <- ZIO.foreach(1 to 10)(n => (RateLimiter.delay *> outputs.offer(n)).fork);
          _ <- adjust(10000 millis);
          contents <- outputs.takeAll
        ) yield assert(contents)(hasSize(equalTo(11)))).provideSomeMagicLayer(testLayer)
      }
    )

}
