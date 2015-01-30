package net.mm.example.services

import net.mm.composer.relations.Relation._

class ReviewService extends FakeService {
  private val reviews = Seq(
    Review(1, 1, "steff", 4, "looks nice"), Review(2, 1, "mark", 3, "expensive"), Review(3, 1, "chris", 5, "awesome, always again"),
    Review(4, 3, "mark", 3, "decent sound, quite ok"), Review(5, 3, "daniel", 3, "awesome price"),
    Review(6, 5, "steff", 1, "bad quality"), Review(7, 5, "mark", 2, "works, but seen much better")
  ).map(r => (r.id, r)).toMap

  val getReviews: Executor[Int, Review] = reviews.filterKeys(_).asFuture
  val getReviewsByProduct: Executor[Int, Seq[Review]] = reviews.values.toSeq.groupBy(_.productId).filterKeys(_).asFuture
  val getReviewsByUser: Executor[String, Seq[Review]] = reviews.values.toSeq.groupBy(_.reviewerId).filterKeys(_).asFuture
}

case class Review(id: Int, productId: Int, reviewerId: String, stars: Int, review: String)