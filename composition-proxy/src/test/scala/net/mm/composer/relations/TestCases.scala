package net.mm.composer.relations

import net.mm.composer.FutureSupport._
import net.mm.composer.relations.Relation._

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

  private val products = Seq(
    Product(1, "computer", "laptop"), Product(2, "computer", "hardware", "storage"),
    Product(3, "audio", "speaker"), Product(4, "audio", "car"), Product(5, "audio"),
    Product(6, "bag", "laptop")
  ).map(p => (p.id, p)).toMap

  private val reviews = Seq(
    Review(1, 1, "steff"), Review(2, 1, "mark"), Review(3, 1, "chris"),
    Review(4, 3, "mark"), Review(5, 3, "daniel"),
    Review(6, 5, "steff"), Review(7, 5, "mark")
  ).map(r => (r.id, r)).toMap

  val allProducts = products.values.toSeq
  val allProductIds = products.values.map(_.id).toSet
  val getProducts: Source[Int, Product] = products.filterKeys(_).asFuture
  val getProductsByCategories: Source[String, Seq[Product]] = _.map(cat => (cat, allProducts.filter(_.categoryIds.contains(cat)))).filterNot(_._2.isEmpty).toMap.asFuture

  val getReviews: Source[Int, Review] = reviews.filterKeys(_).asFuture
  val getReviewsByProduct: Source[Int, Seq[Review]] = reviews.values.toSeq.groupBy(_.productId).filterKeys(_).asFuture
  val getReviewsByUser: Source[String, Seq[Review]] = reviews.values.toSeq.groupBy(_.reviewerId).filterKeys(_).asFuture

  val allCategories = allProducts.flatMap(_.categoryIds).toSet
  val getCategories: Source[String, Category] = _.map(c => (c, Category(c))).toMap.asFuture
  val getCategoriesByProduct: Source[Int, Seq[Category]] = products.filterKeys(_).mapValues(p => p.categoryIds.map(Category)).asFuture
  val getCategorySize: Source[String, Int] = getProductsByCategories(_).map(_.mapValues(_.size))

  val allUsers = reviews.values.map(_.reviewerId).toSet
  val getUsers: Source[String, User] = _.map(u => (u, User(u))).toMap.asFuture

  val mockDataSource = RelationDataSource(
    getProductsByCategories -> getProductsByCategories(allCategories).await,
    getCategorySize -> getCategorySize(allCategories).await,
    getCategories -> getCategories(allCategories).await,
    getReviewsByProduct -> getReviewsByProduct(allProductIds).await,
    getUsers -> getUsers(allUsers).await,
    getProducts -> getProducts(allProductIds).await,
    getCategoriesByProduct -> getCategoriesByProduct(allProductIds).await,
    getReviewsByUser -> getReviewsByUser(allUsers).await
  )
}