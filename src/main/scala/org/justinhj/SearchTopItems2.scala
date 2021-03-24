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

  def app(search: String) = {
    for (
      _ <- putStrLn("Fetching front page stories");
      stories <- Client.getTopStories();
      _ <- putStrLn(s"Received ${stories.itemIDs.size} stories. Searching top $frontPageSize");
      _ <- if(stories.itemIDs.size == 0)
            ZIO.fail("No stories :(")
           else
            for (
              topStories <- ZIO.succeed(stories.itemIDs.take(frontPageSize));
              _ <- ZIO.foreach(topStories){
                itemID =>
                  RateLimiter.delay *> checkItemForString(search,itemID)
              }
            ) yield ()
     ) yield ()
  }

  val config = ZLayer.succeed(RateLimiterConfig(50.milliseconds))

  def program(searchTerm: String) = app(searchTerm).provideMagicLayer(
      Clock.live,
      Console.live,
      HttpClientZioBackend.layer(),
      config,
      RateLimiter.live)

  def run(args: List[String]) = program(args.head).fold(_ => ExitCode.failure, _ => ExitCode.success)
}