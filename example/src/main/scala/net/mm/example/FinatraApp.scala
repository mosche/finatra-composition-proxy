package net.mm.example

import com.twitter.finatra._
import net.mm.composer.relations.execution.SerializationHint.Array
import net.mm.composer.relations.{ToOne, ToMany}
import net.mm.composer.relations.execution.ExecutionHint.NonBijective
import net.mm.composer.{CompositionControllerBuilder, CompositionProxy}
import net.mm.example.services._

object FinatraApp extends FinatraServer
  with CompositionProxy
  with ServicesRegistry {

  System.setProperty("com.twitter.finatra.config.logNode", "")
  System.setProperty("com.twitter.finatra.config.logLevel", "DEBUG")

  lazy val shopController: Controller = CompositionControllerBuilder()
    .register[Category]("categories")
    .as(categoryIdExtractor, productService.getCategories)
    .having(
      "products" -> ToMany(categoryIdExtractor, productService.getProductsByCategories, NonBijective),
      "size" -> ToOne(categoryIdExtractor, productService.getCategorySize)
    )
    .register[Product]("products")
    .as(productIdExtractor, productService.getProducts)
    .having(
      "categories" -> ToOne(categoryIdExtractor, productService.getCategories, Array),
      "reviews" -> ToMany(productIdExtractor, reviewService.getReviewsByProduct)
    )
    .register[Review]("reviews")
    .as(reviewIdExtractor, reviewService.getReviews)
    .having(
      "reviewer" -> ToOne(userIdExtractor, userService.getUsers),
      "product" -> ToOne(productIdExtractor, productService.getProducts),
      "categories" -> ToMany(productIdExtractor, productService.getCategoriesByProduct),
      "comments" -> ToMany(reviewIdExtractor, commentService.getCommentsByReview)
    )
    .register[Comment]("comment")
    .as(commentIdExtractor, commentService.getComments)
    .having(
      "user" -> ToOne(userIdExtractor, userService.getUsers)
    )
    .register[User]("users")
    .as(userIdExtractor, userService.getUsers)
    .having(
      "reviews" -> ToMany(userIdExtractor, reviewService.getReviewsByUser),
      "comments" -> ToMany(userIdExtractor, commentService.getCommentsByUser)
    )
    .buildController("/shop")

  register(shopController)
}
