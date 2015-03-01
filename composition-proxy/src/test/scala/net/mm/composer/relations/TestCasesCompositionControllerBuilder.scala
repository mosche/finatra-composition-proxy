package net.mm.composer.relations

import net.mm.composer.CompositionControllerBuilder
import net.mm.composer.relations.execution.ExecutionHint.NonBijective
import net.mm.composer.relations.execution.SerializationHint.Array

trait TestCasesCompositionControllerBuilder {
  self: TestCases =>

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
