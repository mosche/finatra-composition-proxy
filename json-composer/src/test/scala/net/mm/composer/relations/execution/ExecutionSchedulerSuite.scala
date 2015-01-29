package net.mm.composer.relations.execution

import net.mm.composer.FutureSupport._
import net.mm.composer.relations._
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class ExecutionSchedulerSuite extends FunSuite with TestCases {

  val users = Seq(User("steff"), User("mark"))
  val usernames = users.map(_.username).toSet

  private def scheduler = new ExecutionSchedulerImpl

  test("empty execution plan") {
    scheduler.run(users)(Seq.empty).await shouldBe RelationDataSource()
  }

  test("users with myreviews (ToMany)") {
    val plan = Seq(TaskNode("myreviews", ToMany(Keys.userKey, getReviewsByUser)))

    val result = scheduler.run(users)(plan).await
    val expected = RelationDataSource(
      getReviewsByUser -> getReviewsByUser(usernames).await
    )

    result shouldBe expected
  }

  test("users with myreviews (ToMany) and myreviews->product (ToOne)") {
    val plan = Seq(TaskNode("myreviews", ToMany(Keys.userKey, getReviewsByUser), TaskNode("product", ToOne(Keys.productKey, getProducts))))

    val result = scheduler.run(users)(plan).await

    val reviews = getReviewsByUser(usernames).await
    val productIds = reviews.values.flatten.map(_.productId).toSet
    val expected = RelationDataSource(
      getReviewsByUser -> reviews,
      getProducts -> getProducts(productIds).await
    )

    result shouldBe expected
  }

  test("load multiple nested relations from 'to many'"){
    val plan = Seq(
      TaskNode("reviews", ToMany(Keys.productKey, getReviewsByProduct),
        TaskNode("categories", ToMany(Keys.productKey, getCategoriesByProduct)),
        TaskNode("reviewer", ToOne(Keys.userKey, getUsers))
      )
    )

    val result = scheduler.run(allProducts)(plan).await

    val productIds = allProducts.map(_.id).toSet
    val reviewsMap = getReviewsByProduct(productIds).await
    val reviews = reviewsMap.values.flatten.toSet

    val expected = RelationDataSource(
      getReviewsByProduct -> reviewsMap,
      getCategoriesByProduct -> getCategoriesByProduct(reviews.map(_.productId)).await,
      getUsers ->  getUsers(reviews.map(_.reviewerId)).await
    )

    result shouldBe expected
  }

  test("joind execution on dependend relalation 'categories'"){
    val plan = Seq(
      TaskNode("categories", ToMany(Keys.productKey, getCategoriesByProduct)),
      TaskNode("reviews", ToMany(Keys.productKey, getReviewsByProduct),
        TaskNode("categories", ToMany(Keys.productKey, getCategoriesByProduct))
      )
    )

    val result = scheduler.run(allProducts)(plan).await

    val productIds = allProducts.map(_.id).toSet

    val expected = RelationDataSource(
      getReviewsByProduct -> getReviewsByProduct(productIds).await,
      getCategoriesByProduct -> getCategoriesByProduct(productIds).await
    )

    result shouldBe expected
  }
}
