package net.mm.composer.relations.execution

import net.mm.composer.FutureSupport._
import net.mm.composer.relations._
import org.mockito.Mockito.when
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import net.mm.composer.FutureSupport._

class ExecutionSchedulerSuite extends FunSuite with TestCases {

  val users = Map(
    "steff" -> User("steff"),
    "mark" -> User("mark")
  )
  
  val reviewsByUser = Map(
    "steff" -> Seq(Review(1, 1, "steff"), Review(6, 5, "steff")),
    "mark" -> Seq(Review(7, 5, "mark"))
  )

  val reviewsByProduct = Map(
    1 -> Seq(Review(1, 1, "steff")),
    5 -> Seq(Review(7, 5, "mark"), Review(6, 5, "steff"))
  )
  
  val products = Map(
    1 -> Product(1, "computer", "laptop"),
    5 -> Product(5, "audio")
  )

  val categories = Map(
    "computer" -> Category("computer"),
    "laptop" -> Category("laptop"),
    "audio" -> Category("audio")
  )

  val categoriesByProduct = Map(
    1 -> Seq(Category("computer"), Category("laptop")),
    5 -> Seq(Category("audio"))
  )
  
  private def scheduler = new ExecutionSchedulerImpl

  test("empty execution plan") {
    scheduler.run(users.values)(Seq.empty).await shouldBe RelationDataSource()
  }

  test("users with myreviews (ToMany)") {
    when(getReviewsByUser.apply(reviewsByUser.keySet)).thenReturn(reviewsByUser.asFuture)
    
    val plan = Seq(TaskNode("myreviews", ToMany(userIdExtractor, getReviewsByUser)))

    val result = scheduler.run(users.values)(plan).await

    result shouldBe RelationDataSource(
      getReviewsByUser -> reviewsByUser
    )
  }

  test("users with myreviews (ToMany) and myreviews->product (ToOne)") {
    when(getReviewsByUser.apply(reviewsByUser.keySet)).thenReturn(reviewsByUser.asFuture)
    when(getProducts.apply(products.keySet)).thenReturn(products.asFuture)

    val plan = Seq(TaskNode("myreviews", ToMany(userIdExtractor, getReviewsByUser), TaskNode("product", ToOne(productIdExtractor, getProducts))))

    val result = scheduler.run(users.values)(plan).await

    result shouldBe RelationDataSource(
      getReviewsByUser -> reviewsByUser,
      getProducts -> products
    )
  }

  test("load multiple nested relations from 'to many'"){
    when(getReviewsByProduct.apply(reviewsByProduct.keySet)).thenReturn(reviewsByProduct.asFuture)
    when(getUsers.apply(users.keySet)).thenReturn(users.asFuture)
    when(getCategoriesByProduct.apply(categoriesByProduct.keySet)).thenReturn(categoriesByProduct.asFuture)

    val plan = Seq(
      TaskNode("reviews", ToMany(productIdExtractor, getReviewsByProduct),
        TaskNode("categories", ToMany(productIdExtractor, getCategoriesByProduct)),
        TaskNode("reviewer", ToOne(userIdExtractor, getUsers))
      )
    )

    val result = scheduler.run(products.values)(plan).await
    
    result shouldBe RelationDataSource(
      getReviewsByProduct -> reviewsByProduct,
      getCategoriesByProduct -> categoriesByProduct,
      getUsers -> users
    )
  }

  test("joind execution on dependend relalation 'categories'"){
    when(getCategoriesByProduct.apply(categoriesByProduct.keySet)).thenReturn(categoriesByProduct.asFuture)
    when(getReviewsByProduct.apply(reviewsByProduct.keySet)).thenReturn(reviewsByProduct.asFuture)

    val plan = Seq(
      TaskNode("categories", ToMany(productIdExtractor, getCategoriesByProduct)),
      TaskNode("reviews", ToMany(productIdExtractor, getReviewsByProduct),
        TaskNode("categories", ToMany(productIdExtractor, getCategoriesByProduct))
      )
    )

    val result = scheduler.run(products.values)(plan).await

    result shouldBe RelationDataSource(
      getReviewsByProduct -> reviewsByProduct,
      getCategoriesByProduct -> categoriesByProduct
    )
  }
}
