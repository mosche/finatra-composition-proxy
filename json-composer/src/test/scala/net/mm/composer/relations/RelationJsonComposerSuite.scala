package net.mm.composer.relations

import com.twitter.util.Future
import net.mm.composer.FutureSupport._
import net.mm.composer.properties.{FieldProperty, RelationProperty}
import net.mm.composer.relations.execution.{ExecutionPlan, ExecutionPlanBuilder, ExecutionScheduler}
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar.mock

class RelationJsonComposerSuite extends FunSuite with TestCases with TestCasesRelationRegistry {

  val composer = new RelationJsonComposerImpl()(
    mock[ExecutionScheduler](MockAnswer(Future(mockDataSource))),
    mock[ExecutionPlanBuilder](MockAnswer[ExecutionPlan]()),
    relationRegistry
  )

  test("compose object with flat properties"){
    val tree = RelationProperty("product", FieldProperty("id"), FieldProperty("categoryIds"))
    val obj = Product(1,"book", "ebook")

    val json = composer.compose(obj, classOf[Product])(tree).await
    json.toString shouldBe """{"id":1,"categoryIds":["book","ebook"]}"""
  }

  test("filtering of field properties"){
    val tree = RelationProperty("product", FieldProperty("id"))
    val obj = Product(1,"book", "ebook")

    val json = composer.compose(obj, classOf[Product])(tree).await
    json.toString shouldBe """{"id":1}"""
  }

  test("compose sequence with nested 'to one' relation property"){
    val tree = RelationProperty("reviews", FieldProperty("id"),
      RelationProperty("reviewer", FieldProperty("username"))
    )
    val seq = Seq(Review(1, 1, "steff"),Review(7, 5, "mark"))

    val json = composer.compose(seq, classOf[Review])(tree).await
    json.toString shouldBe """[{"id":1,"reviewer":{"username":"steff"}},{"id":7,"reviewer":{"username":"mark"}}]"""
  }

  test("compose object with nested 'to one' array relation property"){
    val tree = RelationProperty("product", FieldProperty("id"),
      RelationProperty("categories", FieldProperty("id"))
    )
    val obj = Product(1,"computer", "laptop")

    val json = composer.compose(obj, classOf[Product])(tree).await

    json.toString shouldBe """{"id":1,"categories":[{"id":"computer"},{"id":"laptop"}]}"""
  }

  test("compose object with nested 'to many' relation property"){
    val tree = RelationProperty("product", FieldProperty("id"),
      RelationProperty("reviews", FieldProperty("id"))
    )
    val obj = Product(1,"computer", "laptop")

    val json = composer.compose(obj, classOf[Product])(tree).await
    json.toString shouldBe """{"id":1,"reviews":[{"id":1},{"id":2},{"id":3}]}"""
  }
}
