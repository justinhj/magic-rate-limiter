package org.justinhj.hn

object Data {

  import zio.json._

  type HNUserID = String
  type HNItemID = Int

  val HNMissingItemID: HNItemID = -1
  val HNMissingUserID: HNUserID = ""

  sealed trait HNData

  object HNUser {
    // Json decoder
    implicit val decoder: JsonDecoder[HNUser] = DeriveJsonDecoder.gen[HNUser]
  }

  case class HNUser(
    id: HNUserID, // The user's unique username. Case-sensitive. Required.
    //delay : Int, // Delay in minutes between a comment's creation and its visibility to other users.
    created: Int, // Creation date of the user, in Unix Time.
    karma: Int, // The user's karma.
    about: String, // The user's optional self-description. HTML.
    submitted: List[HNItemID]
  ) extends HNData // List of the user's stories, polls and comments.

  object HNItem {
    // Json decoder
    implicit val decoder: JsonDecoder[HNItem] = DeriveJsonDecoder.gen[HNItem]
  }

  case class HNItem(
    id: HNItemID, // The item's unique id.
    deleted: Boolean = false, // true if the item is deleted.
    `type`: String, // The type of item. One of "job", "story", "comment", "poll", or "pollopt".
    by: HNUserID = HNMissingUserID, // The username of the item's author.
    time: Int, // Creation date of the item, in Unix Time.
    text: String = "", // The comment, story or poll text. HTML.
    dead: Boolean = false, // true if the item is dead.
    parent: HNItemID = HNMissingItemID, // The comment's parent: either another comment or the relevant story.
    poll: HNItemID = HNMissingItemID, // The pollopt's associated poll.
    kids: List[HNItemID] = List(), // The ids of the item's comments, in ranked display order.
    url: String = "", // The URL of the story.
    score: Int = -1, // The story's score, or the votes for a pollopt.
    title: String = "", // The title of the story, poll or job.
    parts: List[HNItemID] = List(), // A list of related pollopts, in display order.
    descendants: Int = 0 // In the case of stories or polls, the total comment count.
  ) extends HNData

  object HNItemIDList {
    implicit val decoder: JsonDecoder[HNItemIDList] =
      JsonDecoder[List[HNItemID]].map(HNItemIDList(_))
  }

  case class HNItemIDList(itemIDs: List[HNItemID]) extends HNData

  object HNSingleItemID {
    implicit val decoder: JsonDecoder[HNSingleItemID] =
      JsonDecoder[HNItemID].map(HNSingleItemID(_))
  }

  case class HNSingleItemID(itemId: HNItemID) extends HNData
}