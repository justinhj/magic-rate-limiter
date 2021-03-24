package org.justinhj

import zio._
import zio.console._
import zio.clock._
import sttp.client3.httpclient.zio._
import hn._
import org.justinhj.ratelimiter2.RateLimiter
import zio.duration._
import zio.magic._

object SearchTopItems2 extends App {

  // Get an item and search various fields for the search string
  // Called for effects only
  def checkItemForString(search: String, id: Data.HNItemID) = {
    for (
      item <- Client.getItem(id);
      _ <- if(item.by.contains(search)) putStrLn(s"Found in author: ${item.by}")
           else if(item.text.contains(search)) putStrLn(s"Found in text: ${item.text}")
           else if(item.title.contains(search)) putStrLn(s"Found in title: ${item.title}")
           else if(item.url.contains(search)) putStrLn(s"Found in url: ${item.url}")
           else ZIO.succeed(())
    ) yield ()
  }

  val frontPageSize = 30

  def app(search: String) = {
    for (
      _ <- putStrLn("Fetching front page stories");
      stories <- Client.getTopStories();
      _ <- putStrLn(s"Received ${stories.itemIDs.size} stories");
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

  val config = ZLayer.succeed(RateLimiter.RateLimiterConfig(50.milliseconds))

  def program(searchTerm: String) = app(searchTerm).provideMagicLayer(
      Clock.live,
      Console.live,
      HttpClientZioBackend.layer(),
      config,
      RateLimiter.live)

//  val test1 = (config >>> RateLimiter.live)

  //val oldFashionedZLayers = Clock.live ++ Console.live ++ HttpClientZioBackend.layer() ++ config ++ RateLimiter.live

  //val oldFashionedRun = app("hello").provideLayer(oldFashionedZLayers)

  def run(args: List[String]) = program(args.head).fold(_ => ExitCode.failure, _ => ExitCode.success)
}