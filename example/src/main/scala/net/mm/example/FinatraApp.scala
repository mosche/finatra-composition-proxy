package net.mm.example

import com.twitter.finatra._
import net.mm.composer.relations.execution.SerializationHint.Array
import net.mm.composer.relations.{ToOne, ToMany}
import net.mm.composer.relations.execution.ExecutionHint.NonBijective
import net.mm.composer.{CompositionControllerBuilder, CompositionProxy}
import net.mm.example.services.{User, Review, Product, Category}

object FinatraApp extends FinatraServer
  with CompositionProxy
  with ServicesRegistry {

  System.setProperty("com.twitter.finatra.config.logNode", "")
  System.setProperty("com.twitter.finatra.config.logLevel", "DEBUG")

  lazy val shopController: Controller = CompositionControllerBuilder()
    .register[Category]("categories")
    .as(categoryKey, productService.getCategories)
    .having(
      "products" -> ToMany(categoryKey, productService.getProductsByCategories, NonBijective),
      "size" -> ToOne(categoryKey, productService.getCategorySize)
    )
    .register[Product]("products")
    .as(productKey, productService.getProducts)
    .having(
      "categories" -> ToOne(categoryKey, productService.getCategories, Array),
      "reviews" -> ToMany(productKey, reviewService.getReviewsByProduct)
    )
    .register[Review]("reviews")
    .as(reviewKey, reviewService.getReviews)
    .having(
      "reviewer" -> ToOne(userKey, userService.getUsers),
      "product" -> ToOne(productKey, productService.getProducts),
      "categories" -> ToMany(productKey, productService.getCategoriesByProduct)
    )
    .register[User]("users")
    .as(userKey, userService.getUsers)
    .having(
      "myreviews" -> ToMany(userKey, reviewService.getReviewsByUser)
    )
    .buildController("/shop")

  register(shopController)
}
