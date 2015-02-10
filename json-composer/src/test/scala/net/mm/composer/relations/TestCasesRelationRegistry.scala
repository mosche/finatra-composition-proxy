package net.mm.composer.relations

import net.mm.composer.relations.TestCases.Keys._
import net.mm.composer.relations.execution.ExecutionHint.NonBijective
import net.mm.composer.relations.execution.SerializationHint.Array

trait TestCasesRelationRegistry {
  self: TestCases =>

  implicit val relationRegistry = RegistryBuilder()
    .register[Category]("categories")
    .as(categoryKey, getCategories)
    .having(
      "products" -> ToMany(categoryKey, getProductsByCategories, NonBijective),
      "size" -> ToOne(categoryKey, getCategorySize)
    )
    .register[Product]("products")
    .as(productKey, getProducts)
    .having(
      "categories" -> ToOne(categoryKey, getCategories, Array),
      "reviews" -> ToMany(productKey, getReviewsByProduct)
    )
    .register[Review]("reviews")
    .as(reviewKey, getReviews)
    .having(
      "reviewer" -> ToOne(userKey, getUsers),
      "product" -> ToOne(productKey, getProducts),
      "categories" -> ToMany(productKey, getCategoriesByProduct)
    )
    .register[User]("users")
    .as(userKey, getUsers)
    .having(
      "myreviews" -> ToMany(userKey, getReviewsByUser)
    )
    .build()
}
