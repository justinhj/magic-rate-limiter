package org.justinhj

import zio._
import zio.console._
import zio.clock._
import sttp.client3.httpclient.zio._
import hn._

object Magicratelimiter extends App {


  val exampleApp = {
    for (
      _ <- putStrLn("Fetching item");
      item <- Client.getItem(26498527);
      _ <- putStrLn(s"Item: $item");
      _ <- putStrLn("Fetching user");
      user <- Client.getUser("justinhj");
      _ <- putStrLn(s"User: $user");
      _ <- putStrLn("Fetching top items");
      topItems <- Client.getTopStories();
      _ <- putStrLn(s"Top stories: $topItems")
     ) yield ()
  }

  val zLayers = Clock.live ++ Console.live ++ HttpClientZioBackend.layer()

  def run(args: List[String]) = exampleApp.provideLayer(zLayers).
    fold(_ =>ExitCode(1), _ => ExitCode(0))
}