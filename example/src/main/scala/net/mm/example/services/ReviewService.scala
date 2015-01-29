package net.mm.example.services

import net.mm.composer.relations.Relation._

class ReviewService extends FakeService {
  private val reviews = Seq(
    Review(1, 1, "steff"), Review(2, 1, "mark"), Review(3, 1, "chris"),
    Review(4, 3, "mark"), Review(5, 3, "daniel"),
    Review(6, 5, "steff"), Review(7, 5, "mark")
  ).map(r => (r.id, r)).toMap

  val getReviews: Executor[Int, Review] = reviews.filterKeys(_).asFuture
  val getReviewsByProduct: Executor[Int, Seq[Review]] = reviews.values.toSeq.groupBy(_.productId).filterKeys(_).asFuture
  val getReviewsByUser: Executor[String, Seq[Review]] = reviews.values.toSeq.groupBy(_.reviewerId).filterKeys(_).asFuture
}

case class Review(id: Int, productId: Int, reviewerId: String)