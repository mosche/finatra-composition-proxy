package net.mm.composer.relations

import net.mm.composer.CompositionControllerBuilder
import net.mm.composer.relations.Relation._
import net.mm.composer.relations.execution.ExecutionHint.NonBijective
import net.mm.composer.relations.execution.SerializationHint.Array
import org.scalatest.mock.MockitoSugar._

case class Product(id: Int, categoryIds: String*)

case class Category(id: String)

case class Review(id: Int, productId: Int, reviewerId: String)

case class User(username: String)

trait Extractors {
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
}

trait Sources{
  def getProducts: Source[Int, Product] = ???
  def getReviewsByProduct: Source[Int, Seq[Review]] = ???
  def getReviewsByUser: Source[String, Seq[Review]] = ???
  def getUsers: Source[String, User] = ???
  def getProductsByCategories: Source[String, Seq[Product]] = ???
  def getCategorySize: Source[String, Int] = ???
  def getCategoriesByProduct: Source[Int, Seq[Category]] = ???
  def getReviews: Source[Int, Review] = ???
  def getCategories: Source[String, Category] = ???
}

trait MockedSources extends Sources{
  override val getProducts = mock[Source[Int, Product]]("getProducts")
  override val getProductsByCategories = mock[Source[String, Seq[Product]]]("getProductsByCategories")

  override val getReviews = mock[Source[Int, Review]]("getReviews")
  override val getReviewsByProduct = mock[Source[Int, Seq[Review]]]("getReviewsByProduct")
  override val getReviewsByUser = mock[Source[String, Seq[Review]]]("getReviewsByUser")

  override val getCategories = mock[Source[String, Category]]("getCategories")
  override val getCategoriesByProduct = mock[Source[Int, Seq[Category]]]("getCategoriesByProduct")
  override val getCategorySize = mock[Source[String, Int]]("getCategorySize")

  override val getUsers = mock[Source[String, User]]("getUsers")
}

trait MockCompositionControllerBuilder {
  self: Extractors with Sources =>

  lazy val compositionControllerBuilder = CompositionControllerBuilder()
    .register[Category]("categories")
    .as(categoryIdExtractor, getCategories)
    .having(
      "products" -> ToMany(categoryIdExtractor, getProductsByCategories, NonBijective),
      "size" -> ToOne(categoryIdExtractor, getCategorySize)
    )
    .register[Product]("products")
    .as(productIdExtractor, getProducts)
    .having(
      "categories" -> ToOne(categoryIdExtractor, getCategories, Array),
      "reviews" -> ToMany(productIdExtractor, getReviewsByProduct)
    )
    .register[Review]("reviews")
    .as(reviewIdExtractor, getReviews)
    .having(
      "reviewer" -> ToOne(userIdExtractor, getUsers),
      "product" -> ToOne(productIdExtractor, getProducts),
      "categories" -> ToMany(productIdExtractor, getCategoriesByProduct)
    )
    .register[User]("users")
    .as(userIdExtractor, getUsers)
    .having(
      "myreviews" -> ToMany(userIdExtractor, getReviewsByUser)
    )

  implicit lazy val relationRegistry = compositionControllerBuilder.buildRegistry()
}