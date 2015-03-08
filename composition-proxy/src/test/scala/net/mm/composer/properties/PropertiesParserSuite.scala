package net.mm.composer.properties

import org.scalatest.FunSuite
import org.scalatest.Matchers._
import Modifier._

class PropertiesParserSuite extends FunSuite {

  test("relation with field") {
    val parser = new PropertiesParserImpl

    parser.apply("username") shouldBe Right(
      Seq(FieldProperty("username"))
    )
  }

  test("nested relations") {
    val parser = new PropertiesParserImpl

    val res = parser.apply("title, description, reviews(rating, text, reviewer(username, avatar), product(title))")
    res shouldBe Right(
      Seq(FieldProperty("title"), FieldProperty("description"),
        RelationProperty("reviews", FieldProperty("rating"), FieldProperty("text"),
          RelationProperty("reviewer", FieldProperty("username"), FieldProperty("avatar")),
          RelationProperty("product", FieldProperty("title"))
        )
      )
    )
  }

  test("relation with empty modifiers") {
    val parser = new PropertiesParserImpl(Modifier[Int]("limit"))

    parser.apply("reviews(id)") shouldBe Right(
      Seq(RelationProperty("reviews", FieldProperty("id")))
    )
  }

  test("relation with modifiers") {
    val parser = new PropertiesParserImpl(Modifier[Int]("limit"))

    parser.apply("reviews[limit:10](id)") shouldBe Right(
      Seq(RelationProperty("reviews", Map("limit" -> 10), Seq(FieldProperty("id"))))
    )
  }

  test("usage of invalid modifier") {
    val parser = new PropertiesParserImpl(Modifier[Int]("limit"), Modifier[String]("order"))

    parser.apply("reviews[sorting:DESC](id)") match {
      case Left(msg) => msg should include("Expected: limit, order")
      case other => fail(s"Expected error, but was $other")
    }
  }

  test("usage of invalid modifier value") {
    val parser = new PropertiesParserImpl(Modifier[Int]("limit"), Modifier[String]("order"))

    parser.apply("reviews[limit:DESC](id)") match {
      case Left(msg) => msg should include("limit: int value expected")
      case other => fail(s"Expected error, but was $other")
    }
  }

  test("general syntax error") {
    val parser = new PropertiesParserImpl

    parser.apply("reviews(id") match {
      case Left(msg) => msg should include("expected")
      case other => fail(s"Expected error, but was $other")
    }
  }
}
