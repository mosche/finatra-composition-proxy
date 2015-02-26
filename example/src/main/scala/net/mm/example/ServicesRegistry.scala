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
    case c: Comment => c.userId
  }

  val categoryKey = RelationKey {
    case p: Product => p.categoryIds
    case c: Category => Some(c.id)
  }

  val reviewKey = RelationKey.lift {
    case r: Review => r.id
    case c: Comment => c.reviewId
  }

  val commentKey = RelationKey.lift {
    case c: Comment => c.id
  }

  val userService = new UserService
  val productService = new ProductService
  val reviewService = new ReviewService
  val commentService = new CommentService
}
