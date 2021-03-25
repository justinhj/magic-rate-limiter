package org.justinhj.hn

// Test the Hacker News client using Sttp's test ZLayer

import org.justinhj.ratelimiter2.{RateLimiterConfig,RateLimiter}

import sttp.client3.httpclient.zio._
import sttp.client3.httpclient.zio.stubbing._

import zio.ZLayer
import zio.clock.Clock
import zio.console._
import zio.duration._
import zio.magic._
import zio.test.Assertion._
import zio.test._


object ClientSpec extends DefaultRunnableSpec {

  val configLayer = ZLayer.succeed(RateLimiterConfig(50.milliseconds))

  val testLayer = ZLayer.fromMagic[SttpClient with SttpClientStubbing with Console with Clock](
      Clock.live,
      Console.live,
      configLayer,
      RateLimiter.live,
      HttpClientZioBackend.stubLayer)

  val maxItemStub = for {
    _ <- whenRequestMatches(_.uri.toString.endsWith("maxitem.json")).thenRespond("26575791")
  } yield ()

  def spec = suite("ClientSpec")(
      testM("getMaxItem retrieves and parses correct value") {
        (for {
          maxItem <- maxItemStub *> Client.getMaxItem()
        } yield assert(maxItem)(equalTo(Data.HNSingleItemID(26575791)))).provideLayer(testLayer)
      }
    )

}