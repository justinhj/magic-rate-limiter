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

  val configLayer = ZLayer.succeed(RateLimiter.Config(50.milliseconds))

  val testLayer = ZLayer.fromMagic[SttpClient with SttpClientStubbing with Console with Clock](
      Clock.live,
      Console.live,
      configLayer,
      RateLimiter.live,
      HttpClientZioBackend.stubLayer)

  val sampleUserJSON = """{
    "about" : "This is a test",
    "created" : 1173923446,
    "id" : "jl",
    "karma" : 4240,
    "submitted" : [ 25172559, 25172553]
  }""".stripMargin

  val happyPathStub = for {
    _ <- whenRequestMatches(_.uri.toString.endsWith("maxitem.json")).thenRespond("26575791")
    _ <- whenRequestMatches(_.uri.toString.endsWith("topstories.json")).thenRespond("[ 26583791, 26582729, 26580926, 26580477 ]")
    _ <- whenRequestMatches(_.uri.toString.endsWith("user/jl.json")).thenRespond(sampleUserJSON)
  } yield ()

  def spec = suite("ClientSpec")(
      testM("getMaxItem retrieves and parses correctly") {
        (for {
          maxItem <- happyPathStub *> Client.getMaxItem()
        } yield assert(maxItem)(equalTo(Data.HNSingleItemID(26575791)))).provideLayer(testLayer)
      },
      testM("getTopStories retrieves and parses correctly") {
        (for {
          maxItem <- happyPathStub *> Client.getTopStories()
        } yield assert(maxItem)(equalTo(Data.HNItemIDList(List(26583791, 26582729, 26580926, 26580477))))).provideLayer(testLayer)
      },
      testM("getUser retrieves and parses correctly") {
        (for {
          maxItem <- happyPathStub *> Client.getUser("jl")
        } yield assert(maxItem)(equalTo(Data.HNUser("jl", 1173923446, 4240, "This is a test", List(25172559,25172553))))).provideLayer(testLayer)
      }
    )

}