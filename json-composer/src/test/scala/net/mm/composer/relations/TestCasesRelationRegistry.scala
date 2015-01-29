package net.mm.composer.relations

import net.mm.composer.relations.TestCases.Keys._
import net.mm.composer.relations.execution.ExecutionHint.NonBijective
import net.mm.composer.relations.execution.SerializationHint.Array

trait TestCasesRelationRegistry {
  self: TestCases =>

  implicit val relationRegistry = RegistryBuilder()
    .register[Category](
      "products" -> ToMany(categoryKey, getProductsByCategories, NonBijective),
      "size" -> ToOne(categoryKey, getCategorySize)
    )
    .register[Product](
      "categories" -> ToOne(categoryKey, getCategories, Array),
      "reviews" -> ToMany(productKey, getReviewsByProduct)
    )
    .register[Review](
      "reviewer" -> ToOne(userKey, getUsers),
      "product" -> ToOne(productKey, getProducts),
      "categories" -> ToMany(productKey, getCategoriesByProduct)
    )
    .register[User](
      "myreviews" -> ToMany(userKey, getReviewsByUser)
    )
    .build()
}
