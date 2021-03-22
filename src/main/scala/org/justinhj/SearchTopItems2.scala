package org.justinhj

import zio._
import zio.console._
import zio.clock._
import sttp.client3.httpclient.zio._
import hn._
import org.justinhj.ratelimiter2.RateLimiter

object SearchTopItems2 extends App {

  // This step can be developed from step 1 by implementing the rate limit of 1 second

  // Get an item and search various fields for the search string
  // Called for effects only
  def checkItemForString(search: String, id: Data.HNItemID) = {
    for (
      item <- Client.getItem(id);
      //_ <- putStrLn(item.id + " " + item.title);
      _ <- if(item.by.contains(search)) putStrLn(s"Found in author: ${item.by}")
           else if(item.text.contains(search)) putStrLn(s"Found in text: ${item.text}")
           else if(item.title.contains(search)) putStrLn(s"Found in title: ${item.title}")
           else if(item.url.contains(search)) putStrLn(s"Found in url: ${item.url}")
           else ZIO.succeed(())
    ) yield ()
  }

  val frontPageSize = 10 // The real page has 30

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

  val zLayers = Clock.live ++ Console.live ++ HttpClientZioBackend.layer() ++ RateLimiter.live

  def run(args: List[String]) =
    (for (
      search <- ZIO.fromOption(args.headOption);
      _ <- putStrLn(s"""Searching top items for string "$search" (case sensitive)""");
      _ <- app(search)
    ) yield ()).provideLayer(zLayers).
    fold(_ =>ExitCode(1), _ => ExitCode(0))
}