package org.justinhj.hn

import sttp.client3._
import zio.console._
import zio._
import zio.clock.Clock
import sttp.client3.httpclient.zio._
import sttp.model.StatusCode
import sttp.model.HeaderNames
//import java.time.Duration
import sttp.model.Uri
//import java.time.Instant
import zio.json.JsonDecoder

object Client {

  // These headers are sent with every request
  val commonHeaders = Map("Content-Type" -> "application/json")

  // See here for Hacker News API
  // https://github.com/HackerNews/API

  // Base URL for all requests
  private val baseHNURL = "https://hacker-news.firebaseio.com/v0/"

  // URI builders for each endpoint

  private def getUserURI(userId: Data.HNUserID) = uri"${baseHNURL}user/$userId.json"
  private def getItemURI(itemId: Data.HNItemID) = uri"${baseHNURL}item/$itemId.json"

  private val getTopStoriesURI = uri"${baseHNURL}topstories.json"
  private val getNewStoriesURI = uri"${baseHNURL}newstories.json"
  private val getBestStoriesURI = uri"${baseHNURL}beststories.json"

  private val getMaxItemURI = uri"${baseHNURL}maxitem.json"

  // Methods to build and execute the requests

  def createRequest[T <: Data.HNData](uri: Uri, lastModified: Option[String])
    (implicit D : JsonDecoder[T])
     = {
    val headers = lastModified match {
      case Some(lm) =>
        commonHeaders + ("If-Modified-Since" -> lm)
      case None => commonHeaders
    }

    basicRequest.get(uri).headers(headers).mapResponse {
      case Left(err) => Left(err)
      case Right(body) => {
        D.decodeJson(body) match {
          case Left(err) => Left("Error parsing response: " + err)
          case r => r
        }
      }
    }
   }

  def getObject[T <: Data.HNData](uri: Uri)
    (implicit D : JsonDecoder[T]): ZIO[
      SttpClient with Console with Clock,
      Throwable,
      T
    ] = {

    for (
      response <- send(createRequest[T](uri, None));
      body <- if (response.code == StatusCode.NotModified) {
          putStrLn(
            s"Not modified since ${response.header(HeaderNames.LastModified)}"
          ) *>
            ZIO.fail(new Exception("Not modified"))
        } else if (response.code == StatusCode.Ok) {
          response.body match {
            case Right(data) =>
              putStrLn(
                s"got updated $uri. last modified ${response.header(HeaderNames.LastModified)} at ${System.currentTimeMillis()}"
              ) *>
              ZIO.succeed(data)
            case Left(err) =>
              putStrLn(s"Request failed: $err") *>
              ZIO.fail(new Exception(err))

          }
        } else {
          ZIO.fail(new Exception(s"Unexpected response code ${response.code}"))
        }
    ) yield (body)
  }

  def getItem(itemId: Data.HNItemID): ZIO[
    SttpClient with Console with Clock,
    Throwable,
    Data.HNItem
  ] = {
    val uri = getItemURI(itemId)
    getObject[Data.HNItem](uri)
  }

  def getUser(userId: Data.HNUserID): ZIO[
    SttpClient with Console with Clock,
    Throwable,
    Data.HNUser
  ] = {
    val uri = getUserURI(userId)
    getObject[Data.HNUser](uri)
  }

  def getTopStories(): ZIO[
    SttpClient with Console with Clock,
    Throwable,
    Data.HNItemIDList
  ] = getObject[Data.HNItemIDList](getTopStoriesURI)

  def getBestStories(): ZIO[
    SttpClient with Console with Clock,
    Throwable,
    Data.HNItemIDList
  ] = getObject[Data.HNItemIDList](getBestStoriesURI)

  def getNewStories(): ZIO[
    SttpClient with Console with Clock,
    Throwable,
    Data.HNItemIDList
  ] = getObject[Data.HNItemIDList](getNewStoriesURI)

  def getMaxItem(): ZIO[
    SttpClient with Console with Clock,
    Throwable,
    Data.HNSingleItemID
  ] = getObject[Data.HNSingleItemID](getMaxItemURI)


}
