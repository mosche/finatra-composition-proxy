package net.mm.composer.relations

import net.mm.composer.FutureSupport._
import net.mm.composer.MockAnswer
import net.mm.composer.properties.{FieldProperty, RelationProperty}
import net.mm.composer.relations.execution.{ExecutionPlan, ExecutionPlanBuilder, ExecutionScheduler}
import org.mockito.Matchers.{any, eq => equal}
import org.mockito.Mockito.when
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.mock.MockitoSugar.mock

class RelationJsonComposerSuite extends FunSuite with Extractors with MockedSources with MockCompositionControllerBuilder {

  val dataSource = mock[RelationDataSource]

  val composer = new RelationJsonComposerImpl()(
    mock[ExecutionScheduler](MockAnswer(mock[ExecutionScheduler#Runner](MockAnswer(dataSource.asFuture)))),
    mock[ExecutionPlanBuilder](MockAnswer[ExecutionPlan]()),
    relationRegistry
  )

  test("compose object with flat properties"){
    val properties = Seq(FieldProperty("id"), FieldProperty("categoryIds"))
    val obj = Product(1,"book", "ebook")

    val json = composer.compose(obj, classOf[Product])(properties).await
    json.toString shouldBe """{"id":1,"categoryIds":["book","ebook"]}"""
  }

  test("filtering of field properties"){
    val properties = Seq(FieldProperty("id"))
    val obj = Product(1,"book", "ebook")

    val json = composer.compose(obj, classOf[Product])(properties).await
    json.toString shouldBe """{"id":1}"""
  }

  test("compose sequence with nested 'to one' relation property"){
    when(dataSource.get[User, String](any(), equal("steff"))).thenReturn(Some(User("steff")))
    when(dataSource.get[User, String](any(), equal("mark"))).thenReturn(Some(User("mark")))

    val properties = Seq(FieldProperty("id"),
      RelationProperty("reviewer", FieldProperty("username"))
    )
    val seq = Seq(Review(1, 1, "steff"),Review(7, 5, "mark"))

    val json = composer.compose(seq, classOf[Review])(properties).await
    json.toString shouldBe """[{"id":1,"reviewer":{"username":"steff"}},{"id":7,"reviewer":{"username":"mark"}}]"""
  }

  test("compose object with nested 'to one' array relation property"){
    when(dataSource.get[Category, String](any(), equal("computer"))).thenReturn(Some(Category("computer")))
    when(dataSource.get[Category, String](any(), equal("laptop"))).thenReturn(Some(Category("laptop")))

    val properties = Seq(FieldProperty("id"),
      RelationProperty("categories", FieldProperty("id"))
    )
    val obj = Product(1,"computer", "laptop")

    val json = composer.compose(obj, classOf[Product])(properties).await

    json.toString shouldBe """{"id":1,"categories":[{"id":"computer"},{"id":"laptop"}]}"""
  }

  test("compose object with nested 'to many' relation property"){
    when(dataSource.get[Seq[Review], Int](any(), equal(1))).thenReturn(Some(Seq(
      Review(1, 1, "steff"), Review(2, 1, "mark"), Review(3, 1, "chris"))
    ))

    val properties = Seq(FieldProperty("id"),
      RelationProperty("reviews", FieldProperty("id"))
    )
    val obj = Product(1,"computer", "laptop")

    val json = composer.compose(obj, classOf[Product])(properties).await
    json.toString shouldBe """{"id":1,"reviews":[{"id":1},{"id":2},{"id":3}]}"""
  }
}
