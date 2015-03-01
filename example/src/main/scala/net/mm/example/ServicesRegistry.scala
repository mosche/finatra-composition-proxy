package net.mm.example

import net.mm.composer.CompositionProxy
import net.mm.composer.relations.Relation.IdExtractor
import net.mm.example.services._

trait ServicesRegistry {
  self: CompositionProxy =>

  val productIdExtractor = IdExtractor.lift {
    case r: Review => r.productId
    case p: Product => p.id
  }

  val userIdExtractor = IdExtractor.lift {
    case u: User => u.username
    case r: Review => r.reviewerId
    case c: Comment => c.userId
  }

  val categoryIdExtractor = IdExtractor {
    case p: Product => p.categoryIds
    case c: Category => Some(c.id)
  }

  val reviewIdExtractor = IdExtractor.lift {
    case r: Review => r.id
    case c: Comment => c.reviewId
  }

  val commentIdExtractor = IdExtractor.lift {
    case c: Comment => c.id
  }

  val userService = new UserService
  val productService = new ProductService
  val reviewService = new ReviewService
  val commentService = new CommentService
}
