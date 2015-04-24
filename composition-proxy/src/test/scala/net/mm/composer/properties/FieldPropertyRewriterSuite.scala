package net.mm.composer.properties

import net.mm.composer.MockAnswer
import net.mm.composer.relations.Relation.AnyRelation
import net.mm.composer.relations.{RelationRegistry, Review, User}
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class FieldPropertyRewriterSuite extends FunSuite {

  implicit val registry = mock(classOf[RelationRegistry], MockAnswer(None))
  val rewriter = new FieldPropertyRewriterImpl(classOf[Review])


  test("pass through errors") {
    val error = Left("error")
    rewriter.apply(error) shouldBe error
  }

  test("keep fields and relations of clazz") {
    when(registry.get(classOf[Review], "reviewer")).thenReturn(Some(mock(classOf[AnyRelation], MockAnswer(classOf[User]))))

    val properties = Right(Seq(
      FieldProperty("id"),
      FieldProperty("productId"),
      FieldProperty("reviewerId"),
      RelationProperty("reviewer",
        FieldProperty("username")
      )
    ))
    rewriter.apply(properties) shouldBe properties
  }

  test("rewrite relation field as relation") {
    when(registry.get(classOf[Review], "totalreviews")).thenReturn(Some(mock(classOf[AnyRelation], MockAnswer(classOf[Int]))))

    val properties = Right(Seq(
      FieldProperty("id"),
      FieldProperty("totalreviews")
    ))

    rewriter.apply(properties) shouldBe Right(Seq(
      FieldProperty("id"),
      RelationProperty("totalreviews")
    ))
  }

  test("ignore unknowns") {
    val properties = Right(Seq(
      RelationProperty("user")
    ))

    rewriter.apply(properties) shouldBe properties
  }

}
