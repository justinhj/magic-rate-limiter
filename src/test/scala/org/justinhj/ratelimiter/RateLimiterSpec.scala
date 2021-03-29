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

  private val configLayer = ZLayer.succeed(RateLimiter.Config(1000 millis))
  private val testLayer = (TestClock.any ++ configLayer) >>> RateLimiter.live

  def spec: ZSpec[Environment, Failure] =
    suite("RateLimiterSpec")(
      testM("Rate limiter does not limit first request") {
        (for (
          outputs <- Queue.bounded[Int](10);
          _ <- (ZIO.sleep(120 millis) *> outputs.offer(2)).fork;
          _ <- adjust(100 millis);
          _ <- RateLimiter.delay *> outputs.offer(1);
          _ <- outputs.offer(1);
          //_ <- outputs.offer(2);
          _ <- adjust(3000 millis);
          t1 <- currentTime(TimeUnit.MILLISECONDS);
          _ <- putStrLn(s"time is $t1");
          contents <- outputs.takeAll
        ) yield assert(contents)(equalTo(List(1,2)))).provideSomeMagicLayer(testLayer)
      }
    )

}

//
//for {
//_    <- setTime(1.millis)
//time <- currentTime(TimeUnit.NANOSECONDS)
//} yield assert(time)(equalTo(2.millis.toNanos))