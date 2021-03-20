package org.justinhj

import zio._
import zio.console._
import zio.clock._
import sttp.client3.httpclient.zio._
import hn._

object ShowTopItem extends App {

  val app = {
    for (
      _ <- putStrLn("Fetching stories");
      stories <- Client.getTopStories();
      _ <- putStrLn(s"Received ${stories.itemIDs.size} stories");
      _ <- if(stories.itemIDs.size == 0)
            ZIO.fail("No stories :(")
           else
            for (
              item <- Client.getItem(stories.itemIDs.head);
              _ <- putStr(s"Top story ${item.title} ${item.url}")
            ) yield ()
     ) yield ()
  }

  val zLayers = Clock.live ++ Console.live ++ HttpClientZioBackend.layer()

  def run(args: List[String]) = app.provideLayer(zLayers).
    fold(_ =>ExitCode(1), _ => ExitCode(0))
}