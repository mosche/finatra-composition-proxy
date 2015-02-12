package net.mm.example

import net.mm.composer.CompositionProxy
import net.mm.composer.relations.Relation.RelationKey
import net.mm.example.services._

trait ServicesRegistry {
  self: CompositionProxy =>

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

  val reviewKey = RelationKey.lift {
    case r: Review => r.id
  }

  implicit val userService = new UserService
  implicit val productService = new ProductService
  implicit val reviewService = new ReviewService
}
