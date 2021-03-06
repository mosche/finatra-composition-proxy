package net.mm.composer.relations.execution

import net.mm.composer.properties.{Property, FieldProperty, RelationProperty}
import net.mm.composer.relations._
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class ExecutionPlanBuilderSuite extends FunSuite with Extractors with MockedSources with MockCompositionControllerBuilder {

  val executionPlan = new ExecutionPlanBuilderImpl

  test("no relation") {
    val properties = Seq.empty
    executionPlan[Product](properties) shouldBe Seq.empty
  }

  test("with reviews relation") {
    val properties = Seq(RelationProperty("reviews"))
    executionPlan[Product](properties) shouldBe Seq(TaskNode("reviews", relationRegistry.get(classOf[Product], "reviews").get))
  }

  test("optimized execution plan on Id extractor") {
    val properties = Seq(RelationProperty("reviews", RelationProperty("categories")))
    val plan = executionPlan[Product](properties)

    plan shouldBe Seq(
      TaskNode("categories", relationRegistry.get(classOf[Review], "categories").get),
      TaskNode("reviews", relationRegistry.get(classOf[Product], "reviews").get)
    )
  }

  test("optimized execution plan on Id extractor with cost based sorting") {
    val properties = Seq(RelationProperty("reviews", RelationProperty("categories", RelationProperty("size"))))
    val plan = executionPlan[Product](properties)

    plan shouldBe Seq(
      TaskNode("reviews", relationRegistry.get(classOf[Product], "reviews").get),
      TaskNode("categories", relationRegistry.get(classOf[Review], "categories").get,
        TaskNode("size", relationRegistry.get(classOf[Category], "size").get)
      )
    )
  }

  test("execution plan with leaf nodes") {
    val properties = Seq[Property](FieldProperty("title"), FieldProperty("description"),
      RelationProperty("reviews", FieldProperty("rating"), FieldProperty("text"),
        RelationProperty("reviewer", FieldProperty("username"), FieldProperty("avatar"))
      )
    )

    val plan = executionPlan[Product](properties)

    plan shouldBe Seq(
      TaskNode("reviews", relationRegistry.get(classOf[Product], "reviews").get,
        TaskNode("reviewer", relationRegistry.get(classOf[Review], "reviewer").get)
      )
    )
  }
}
