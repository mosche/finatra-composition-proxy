package net.mm.composer.relations

import net.mm.composer.relations.Relation._
import org.scalatest.mock.MockitoSugar._

case class Product(id: Int, categoryIds: String*)

case class Category(id: String)

case class Review(id: Int, productId: Int, reviewerId: String)

case class User(username: String)

trait TestCases {

  val productIdExtractor = IdExtractor.lift {
    case r: Review => r.productId
    case p: Product => p.id
  }

  val userIdExtractor = IdExtractor.lift {
    case u: User => u.username
    case r: Review => r.reviewerId
  }

  val categoryIdExtractor = IdExtractor {
    case p: Product => p.categoryIds
    case c: Category => Some(c.id)
  }

  val reviewIdExtractor = IdExtractor.lift {
    case r: Review => r.id
  }

  val getProducts = mock[Source[Int, Product]]("getProducts")
  val getProductsByCategories = mock[Source[String, Seq[Product]]]("getProductsByCategories")

  val getReviews = mock[Source[Int, Review]]("getReviews")
  val getReviewsByProduct = mock[Source[Int, Seq[Review]]]("getReviewsByProduct")
  val getReviewsByUser = mock[Source[String, Seq[Review]]]("getReviewsByUser")

  val getCategories = mock[Source[String, Category]]("getCategories")
  val getCategoriesByProduct = mock[Source[Int, Seq[Category]]]("getCategoriesByProduct")
  val getCategorySize = mock[Source[String, Int]]("getCategorySize")

  val getUsers = mock[Source[String, User]]("getUsers")
}