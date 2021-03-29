package org.justinhj

import zio._
import zio.console._
import zio.clock._
import sttp.client3.httpclient.zio._
import hn._
import org.justinhj.ratelimiter2._
import zio.duration._
import zio.magic._

object SearchTopItems2 extends App {

  // Get an item and search various fields for the search string
  // Called for effects only
  def checkItemForString(search: String, id: Data.HNItemID) = {
    val searchLower = search.toLowerCase
    for (
      item <- Client.getItem(id);
      _ <- if(item.by.toLowerCase.contains(searchLower)) putStrLn(s"Found in author: ${item.by}")
           else if(item.text.toLowerCase.contains(searchLower)) putStrLn(s"Found in text: ${item.text}")
           else if(item.title.toLowerCase.contains(searchLower)) putStrLn(s"Found in title: ${item.title}")
           else if(item.url.toLowerCase.contains(searchLower)) putStrLn(s"Found in url: ${item.url}")
           else ZIO.succeed(())
    ) yield ()
  }

  val frontPageSize = 30

  def app(search: String): ZIO[RateLimiter with SttpClient with Console with Clock,Throwable,Unit] = {
    for (
      _ <- putStrLn("Fetching front page stories");
      stories <- Client.getTopStories();
      _ <- putStrLn(s"Received ${stories.itemIDs.size} stories. Searching top $frontPageSize");
      _ <- ZIO.when(stories.itemIDs.size > 0)(for (
              topStories <- ZIO.succeed(stories.itemIDs.take(frontPageSize));
              _ <- ZIO.foreach(topStories){
                itemID =>
                  RateLimiter.delay *> checkItemForString(search,itemID)
              }
            ) yield ())
     ) yield ()
  }

  // Program with no magic
  val configLayer =
    ZLayer.succeed(RateLimiter.Config(50.milliseconds))
  val rateLimiterLayer =
    (Clock.live ++ configLayer >>> RateLimiter.live) ++ HttpClientZioBackend.layer() ++ Console.live

  def program(searchTerm: String): ZIO[ZEnv,Throwable,Unit] =
    app(searchTerm).provideCustomLayer(rateLimiterLayer)

  // Same program with magic
  def programWithMagic(searchTerm: String): ZIO[ZEnv,Throwable,Unit] =
    app(searchTerm).provideMagicLayer(
      Clock.live,
      Console.live,
      HttpClientZioBackend.layer(),
      configLayer,
      RateLimiter.live)

  def run(args: List[String]) = {
    ZIO.when(args.nonEmpty)(program(args.head)).
        fold(_ => ExitCode.failure, _ => ExitCode.success)
  }
}