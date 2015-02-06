package net.mm.composer.relations

import net.mm.composer.relations.TestCases.Keys._
import net.mm.composer.relations.execution.ExecutionHint.NonBijective
import net.mm.composer.relations.execution.SerializationHint.Array

trait TestCasesRelationRegistry {
  self: TestCases =>

  implicit val relationRegistry = RegistryBuilder()
    .register[Category](
      "categories" -> ToOne(categoryKey, getCategories)
    ).having(
      "products" -> ToMany(categoryKey, getProductsByCategories, NonBijective),
      "size" -> ToOne(categoryKey, getCategorySize)
    )
    .register[Product](
      "products" -> ToOne(productKey, getProducts)
    ).having(
      "categories" -> ToOne(categoryKey, getCategories, Array),
      "reviews" -> ToMany(productKey, getReviewsByProduct)
    )
    .register[Review](
      "reviews" -> ToOne(reviewKey, getReviews)
    ).having(
      "reviewer" -> ToOne(userKey, getUsers),
      "product" -> ToOne(productKey, getProducts),
      "categories" -> ToMany(productKey, getCategoriesByProduct)
    )
    .register[User](
      "users" -> ToOne(userKey, getUsers)
    ).having(
      "myreviews" -> ToMany(userKey, getReviewsByUser)
    )
    .build()
}
