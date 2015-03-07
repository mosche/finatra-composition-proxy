package net.mm.composer.properties

import net.mm.composer.properties.PropertiesParser.{PropertiesParser,Error}
import net.mm.composer.properties.TokenParser._

object PropertiesParser {
  type Error = String
  type PropertiesParser = String => Either[Error, Seq[Property]]
}

class PropertiesParserImpl private(optModifiers: Option[Seq[Modifier[_]]] = None) extends PropertiesParser {

  def this(modifiers: Modifier[_]*) = this(if (modifiers.isEmpty) None else Some(modifiers))

  override def apply(str: String): Either[Error, Seq[Property]] = parseAll(properties, str) match {
    case Success(tree, _) => Right(tree)
    case NoSuccess(msg, next) => Left(s"$msg (${next.pos})")
  }

  implicit private class ModifiersSupport(modifiers: Seq[Modifier[_]]) {
    def names = modifiers.map(_.name).mkString(", ")

    def toParser: Parser[(String, Any)] = modifiers.map{ modifier =>
      (s"${modifier.name}:" ~> modifier.parser)
        .withFailureMessage(s"${modifier.name}: ${modifier.valueType} value expected")
        .map((modifier.name, _))
    }.reduce(_ | _)
  }

  private val noModifiers = success(Map.empty[String, Any])
  private val modifiers = optModifiers.fold(noModifiers) { modifiers =>
    ("[" ~> repsep(modifiers.toParser, ",") <~ "]").map(_.toMap)
      .withFailureMessage(s"Expected: ${modifiers.names}")
  }

  private lazy val properties = repsep(relation | field, ",") 

  private val field = ident map FieldProperty
  private val relation: Parser[RelationProperty] = ident ~ (modifiers | noModifiers) ~ ("(" ~> properties <~ ")") map {
    case name ~ modifiers ~ properties => RelationProperty(name, modifiers, properties: _*)
  }
}
