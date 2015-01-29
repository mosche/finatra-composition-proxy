package net.mm.example

import net.mm.composer.ComposingServer
import net.mm.composer.relations.Relation.RelationKey
import net.mm.composer.relations.execution.ExecutionHint.NonBijective
import net.mm.composer.relations.execution.SerializationHint.Array
import net.mm.composer.relations.{RegistryBuilder, RelationRegistry, ToMany, ToOne}
import net.mm.example.services._


trait ServicesRegistry {
  self: ComposingServer =>

  object Keys {
    val productKey = RelationKey.lift {
      case r: Review => r.productId
      case p: Product => p.id
    }

    val userKey = RelationKey.lift {
      case u: User => u.username
      case r: Review => r.reviewerId
    }

    val categoryKey = RelationKey {
      case p: Product => p.categoryIds
      case c: Category => Some(c.id)
    }
  }

  implicit val userService = new UserService
  implicit val productService = new ProductService
  implicit val reviewService = new ReviewService

  implicit def relationRegistry: RelationRegistry = RegistryBuilder()
    .register[Category](
      "products" -> ToMany(Keys.categoryKey, productService.getProductsByCategories, NonBijective),
      "size" -> ToOne(Keys.categoryKey, productService.getCategorySize)
    )
    .register[Product](
      "categories" -> ToOne(Keys.categoryKey, productService.getCategories, Array),
      "reviews" -> ToMany(Keys.productKey, reviewService.getReviewsByProduct)
    )
    .register[Review](
      "reviewer" -> ToOne(Keys.userKey, userService.getUsers),
      "product" -> ToOne(Keys.productKey, productService.getProducts),
      "categories" -> ToMany(Keys.productKey, productService.getCategoriesByProduct)
    )
    .register[User](
      "myreviews" -> ToMany(Keys.userKey, reviewService.getReviewsByUser)
    )
    .build()
}
