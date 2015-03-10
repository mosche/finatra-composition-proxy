# A Finatra composition proxy

[*Finatra*](http://finatra.info/) is a *Scala* web framework inspired by *Sinatra* and build on top of *Twitter-Server*.
As such it provides rich metrics, templating and integrates well with [*Finagle*](https://twitter.github.io/finagle/), Twitter's protocol-agnostic async RPC system.
[*Your Server as a Function*](http://monkey.org/~marius/funsrv.pdf), a great paper by Marius Eriksen, describes the philosophy behind *Finagle* and how *Futures*, *Services* and *Filters* play so well together.

## Microservice composition

The past two years I've seen lots of companies starting to adapt the [*microservice*](http://martinfowler.com/articles/microservices.html) paradigm.
Among the various challenges, there's a lot of discussions around the *composition challenge*, regardless whether services shall be composed in a large website or into a single REST API.

Talking about a single REST endpoint, the goal certainly shouldn't be to transform one monolith into another (your proxy) and build lots of beautiful manageable *microservice* around it.
But dealing with various devices, particularly talking about mobile, there certainly is an advantage in aggregating the various calls to a multitude of services into some few ones.

Already some while ago Twitter presented [*Stitch*](https://www.youtube.com/watch?v=VVpmMfT8aYw), a library for composing *Finagle* services.
*Stitch* provides a concise Scala query API which facilitates a readable expression of application logic hiding the complexity of bulk RPC calls.
That way *Stitch* efficiently allows Twitter to build Services on top of other Services. But, unfortunately, *Stitch* is not open-sourced yet.

[*Clump*](http://getclump.io/), which is deeply inspired by *Stitch*, was just recently open-sourced by developers from SoundCloud.
Similarly to *Stitch* it provides an easy to use declarative approach focusing on *what* to fetch instead of *how* to fetch it.
Performance is then enhanced by means of bulk requests, parallel requests to multiple sources and an underlying caching layer.


## The composition proxy

The composition proxy attempts to take the declarative approach of *Stitch* and *Clump* one step further.
Following a configuration based approach defining *what* can be fetched, the *how* is totally left open.
Instead, based on a configuration, an entire REST API with some nifty features is automatically generated.

#### Example: Controller by configuration

```scala
  // A shop controller registering the following composition resources:
  //
  // GET /shop/categories/:id
  // GET /shop/categories/:id/products
  // GET /shop/categories/:id/size
  // GET /shop/comment/:id
  // GET /shop/products/:id
  // GET /shop/products/:id/reviews
  // GET /shop/reviews/:id
  // GET /shop/reviews/:id/comments
  // GET /shop/users/:id
  // GET /shop/users/:id/comments
  // GET /shop/users/:id/reviews

  lazy val shopController: Controller = CompositionControllerBuilder()
    .register[Category]("categories")
    .as(categoryIdExtractor, productService.getCategories)
    .having(
      "products" -> ToMany(categoryIdExtractor, productService.getProductsByCategories, NonBijective),
      "size" -> ToOne(categoryIdExtractor, productService.getCategorySize)
    )
    .register[Product]("products")
    .as(productIdExtractor, productService.getProducts)
    .having(
      "categories" -> ToOne(categoryIdExtractor, productService.getCategories, Array),
      "reviews" -> ToMany(productIdExtractor, reviewService.getReviewsByProduct)
    )
    .register[Review]("reviews")
    .as(reviewIdExtractor, reviewService.getReviews)
    .having(
      "reviewer" -> ToOne(userIdExtractor, userService.getUsers),
      "product" -> ToOne(productIdExtractor, productService.getProducts),
      "categories" -> ToMany(productIdExtractor, productService.getCategoriesByProduct),
      "comments" -> ToMany(reviewIdExtractor, commentService.getCommentsByReview)
    )
    .register[Comment]("comment")
    .as(commentIdExtractor, commentService.getComments)
    .having(
      "user" -> ToOne(userIdExtractor, userService.getUsers)
    )
    .register[User]("users")
    .as(userIdExtractor, userService.getUsers)
    .having(
      "reviews" -> ToMany(userIdExtractor, reviewService.getReviewsByUser),
      "comments" -> ToMany(userIdExtractor, commentService.getCommentsByUser)
    )
    .buildController("/shop")
```

*Fields* as well as *relations* (by means of a RPC call) are returned on demand in order to address the specific information need as well as limitations of an API client.
That will say service composition is exposed to the client by means of a concise query DSL ([the *properties* DSL](#the-properties-dsl)). Leveraging the query DSL code complexity is significantly reduced both on client as well as the server side.

Every *properties* query is translated into an optimized [*execution plan*](#the-execution-plan) in order to enhance performance as much as possible.
Optimizations taken into account are:

- rearrangement of *relations* in order to maximize parallelism
- bulk requests (if possible even accross multiple composition levels)
- a caching layer

While this approach is more powerful in it's specific usage case, it obviously is less flexible than the generic DSL of *Clump*.

### The properties DSL

Similar to Facebook's [*field expansion (Graph API)*](https://developers.facebook.com/docs/graph-api/using-graph-api/#fieldexpansion), *fields* and *relations* are requested on demand.

Properties are queried according to the following grammar:

- field -> *AlphaNumericIdentifier*
- relation -> *field* '(' *properties* ')' 
- property -> *field* | *relation*
- properties -> ( *property* ',' )* *property*

*Properties* are then appended to the request as a query parameter *properties*, e.g. `?properties=id,title,reviews(stars)`

### The execution plan

Based on a properties tree an optimized *execution plan* is generated according to the following optimizations:

1. Whenever nested relations in the tree are using the same *Id extractor* as their parent relation,
   such relations are moved upwards in the graph to increase parallelism during execution.
   Therefore relations must be invariant on the id. That will say applying the *Id extractor* on the relation result(s) will produce the same Id again.
   If this is not the case for a particular relation it must be marked with the execution hint *NonBijective*.

2. Once subtrees are sorted according to their depth, execution can later be split into two phases:
   From the flattest to the deepest subtree all available Ids for a *relation source* are collected first - even accross multiple levels in the tree.
   Afterwards, depth first from the deepest to the flattest subtree (again respecting the two phases for following subtrees),
   the *batch source executor* will load the data for all collected Ids.
   As relations trees tend to be highly unbalanced this simple execution strategy works really well.


## Example

Just a tiny example illustrating the idea of JSON composition to build up a powerful REST API backed by a *microservice* architecture.

There's actually no remote services used in this example. However, some fake services shall demonstrate the case.

### Run the example

```sbt example/run```

##### Example 1: Load a product with id and title only<br>

```
curl http://localhost:7070/shop/products/1?properties=id,title
```

```javascript
{
 id: 1,
 title: "Apple iBook"
}
```

##### Example 2: Load a product with all its categories, reviews and the reviewer<br>
 
```
curl http://localhost:7070/shop/products/1?properties=id,title,reviews(stars,review,reviewer(username)),categories(id)
```

```javascript
{
 id: 1,
 title: "Apple iBook",
 categories: [
   {
     id: "computer"
   },
   {
     id: "laptop"
   }
 ],
 reviews: [
   {
     stars: 4,
     review: "looks nice",
     reviewer: {
       username: "steff"
     }
   },
   {
     stars: 3,
     review: "expensive",
     reviewer: {
       username: "mark"
     }
   },
   {
     stars: 5,
     review: "awesome, always again",
     reviewer: {
       username: "chris"
     }
   }
 ]
}
```
