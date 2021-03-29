package org.justinhj.ratelimiter2

// Tests for the Ref based rate limiter

import org.justinhj.ratelimiter2.{RateLimiterConfig,RateLimiter}

import zio.ZIO
import zio.ZLayer
import zio.clock._
import zio.test.environment.TestClock._
import zio.duration._
import zio.magic._
import zio.test.Assertion._
import zio.test._
import zio.console._
import zio.test.environment._
import java.util.concurrent.TimeUnit
import zio.Has

object RateLimiterSpec extends DefaultRunnableSpec {

  val configLayer = ZLayer.succeed(RateLimiterConfig(50.milliseconds))

  //val e = TestEnvironment
  private val ohmyfuckinggod = TestClock.any ++ configLayer ++ RateLimiter.live

//  val testLayer = ZLayer.fromMagic[RateLimiter](
//    configLayer,
//    Clock.any,
//    Console.any,
//    RateLimiter.live)

  // Some tests
  // After making the rate limiter we should be able to immediately make a request

  def spec: ZSpec[Environment, Failure] =
    suite("RateLimiterSpec")(
      testM("Rate limiter does not limit first request") {
        (for (
          //_ <- RateLimiter.delay *>
          _ <- ZIO.succeed(true);
          time <- currentTime(TimeUnit.NANOSECONDS)
        ) yield assert(time)(equalTo(2.millis.toNanos)))
      }
    )

}

//
//for {
//_    <- setTime(1.millis)
//time <- currentTime(TimeUnit.NANOSECONDS)
//} yield assert(time)(equalTo(2.millis.toNanos))