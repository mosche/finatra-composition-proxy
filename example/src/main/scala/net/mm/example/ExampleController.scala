package net.mm.example

import com.twitter.finatra.Controller
import net.mm.composer.FinatraResponseComposer
import net.mm.composer.RouteParamsSupport._
import net.mm.composer.properties.PropertiesParser
import net.mm.composer.relations.RelationJsonComposer
import net.mm.example.services._

class ExampleController(implicit productService: ProductService, userService: UserService, reviewService: ReviewService, val propertiesParser: PropertiesParser, val relationComposer: RelationJsonComposer)
  extends Controller with FinatraResponseComposer {

  get("/users/:username") { implicit request =>
    request.routeParams.get("username").fold(render.badRequest.toFuture)(username =>
      userService.getUsers(Set(username)).flatMap(users =>
        users.get(username).fold(render.notFound.toFuture)(render.composedJson[User])
      )
    )
  }

  get("/products") { implicit request =>
    render.composedJson[Product](productService.allProducts)
  }

  get("/products/:id") { implicit request =>
    request.routeParams.getInt("id").fold(render.badRequest.toFuture)(id =>
      productService.getProducts(Set(id)).flatMap(products =>
        products.get(id).fold(render.notFound.toFuture)(render.composedJson[Product])
      )
    )
  }

  get("/products/:id/reviews") { implicit request =>
    request.routeParams.getInt("id").fold(render.badRequest.toFuture)(id =>
      reviewService.getReviewsByProduct(Set(id)).flatMap(reviews =>
        reviews.get(id).fold(render.notFound.toFuture)(render.composedJson[Review])
      )
    )
  }
}
