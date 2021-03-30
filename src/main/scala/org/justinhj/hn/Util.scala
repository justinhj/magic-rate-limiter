package org.justinhj.hn

import org.ocpsoft.prettytime.PrettyTime
import java.net.URL
import java.util.Date
import scala.util.{Failure, Success, Try}

// Some useful shared functions
object Util {

   def timestampToPretty(ts: Int) : String = {

    val epochTimeMS = ts * 1000L

    val p = new PrettyTime()
    p.format(new Date(epochTimeMS))
  }

  // We will display just the hostname of the URL
  // this returns close to what we want but not exactly...
  def getHostName(url: String) : String = {
    if(url.isEmpty) ""
    else {
      Try(new URL(url)) match {
        case Success(u) =>
          "(" + u.getHost + ")"
        case Failure(e) =>
          ""
      }
    }
  }

  // Create a string that shows an item nicely
  def show(item: Data.HNItem): String = {
    s"""${item.id}: ${item.title} ${Util.getHostName(item.url)}
    \t${item.score} points by ${item.by} at ${Util.timestampToPretty(item.time)} ${item.descendants} comments
    \turl ${item.url}

    """.stripMargin
  }

}