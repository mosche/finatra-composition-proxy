package net.mm.example.services

import javax.management.relation.RelationService

import net.mm.composer.relations.Relation.RelationSource

class CommentService extends FakeService {

  private val comments = Seq(
    Comment(1, 1, "mark", "i disagree"),
    Comment(2, 2, "chris", "in deed, it is"),
    Comment(3, 2, "steff", "i can afford it"),
    Comment(4, 4, "mark", "absolutely not expensive for what you get")
  ).map(c => (c.id, c)).toMap

  val getComments: RelationSource[Int, Comment] = comments.filterKeys(_).asFuture
  val getCommentsByReview: RelationSource[Int, Seq[Comment]] = comments.groupValuesBy(_.reviewId).filterKeys(_).asFuture
  val getCommentsByUser: RelationSource[String, Seq[Comment]] = comments.groupValuesBy(_.userId).filterKeys(_).asFuture
}

case class Comment(id: Int, reviewId: Int, userId: String, comment: String)
