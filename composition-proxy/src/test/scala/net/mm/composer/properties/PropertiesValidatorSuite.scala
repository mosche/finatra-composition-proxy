package net.mm.composer.properties

import net.mm.composer.MockAnswer
import net.mm.composer.relations.Relation.AnyRelation
import net.mm.composer.relations.{User, RelationRegistry, Review}
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.mockito.Mockito._

class PropertiesValidatorSuite extends FunSuite {

  implicit val registry = mock(classOf[RelationRegistry], RETURNS_SMART_NULLS)
  val validator = new PropertiesValidatorImpl(classOf[Review])


  test("pass through errors") {
    val error = Left("error")
    validator.apply(error) shouldBe error
  }

  test("validate fields of clazz") {
    val properties = Right(Seq(
      FieldProperty("id"),
      FieldProperty("productId"),
      FieldProperty("reviewerId")
    ))
    validator.apply(properties) shouldBe properties
  }

  test("error on unknown field") {
    when(registry.get(classOf[Review], "unknown")).thenReturn(None)
    val properties = Right(Seq(
      FieldProperty("id"),
      FieldProperty("unknown")
    ))

    validator.apply(properties) shouldBe Left("Unknown field 'unknown' for Review")
  }

  test("validate relation of clazz") {
    when(registry.get(classOf[Review], "reviewer")).thenReturn(Some(mock(classOf[AnyRelation], MockAnswer(classOf[User]))))

    val properties = Right(Seq(
      RelationProperty("reviewer",
        FieldProperty("username")
      )
    ))

    validator.apply(properties) shouldBe properties
  }

  test("validate relation of clazz with nested unknown field") {
    when(registry.get(classOf[Review], "reviewer")).thenReturn(Some(mock(classOf[AnyRelation], MockAnswer(classOf[User]))))
    when(registry.get(classOf[User], "unknown")).thenReturn(None)

    val properties = Right(Seq(
      RelationProperty("reviewer",
        FieldProperty("unknown")
      )
    ))

    validator.apply(properties) shouldBe Left("Unknown field 'unknown' for User")
  }

  test("error on unknown relation") {
    when(registry.get(classOf[Review], "user")).thenReturn(None)

    val properties = Right(Seq(
      RelationProperty("user",
        FieldProperty("username")
      )
    ))

    validator.apply(properties) shouldBe Left("Unknown relation 'user' for Review")
  }

}
