package net.mm.composer.properties

import scala.util.parsing.combinator._

trait PropertiesParser {
  type Error = String

  def apply(properties: String): Either[Error, PropertyTree]
}

class PropertiesParserImpl private(optModifiers: Option[Seq[Modifier[_]]] = None) extends PropertiesParser {

  def this(modifiers: Modifier[_]*) = this(if (modifiers.isEmpty) None else Some(modifiers))

  override def apply(properties: String): Either[Error, PropertyTree] = parser(properties)

  private val parser = new JavaTokenParsers {
    val MInt = manifest[Int]
    val MString = manifest[String]

    implicit class ManifestSupport(m: Manifest[_]) {
      def toParser: Parser[Any] = m match {
        case MInt => "\\d+".r map (_.toInt)
        case MString => ident | stringLiteral
        case m => throw new Exception(s"No parser for $m")
      }
    }

    implicit class ModifiersSupport(seq: Seq[Modifier[_]]) {
      def names = seq.map(_.name).mkString(", ")

      def toParser: Parser[(String, Any)] = seq.map(m =>
        (s"${m.name}:" ~> m.manifest.toParser).map((m.name, _))
          .withFailureMessage(s"${m.name}: ${m.manifest.runtimeClass} value expected")
      ).reduce(_ | _)
    }

    // fail fast for unsupported modifiers
    optModifiers.foreach(_.foreach(_.manifest.toParser))

    val noModifiers = success(Map.empty[String, Any])
    val modifiers = optModifiers.fold(noModifiers) { modifiers =>
      ("[" ~> repsep(modifiers.toParser, ",") <~ "]").map(_.toMap)
        .withFailureMessage(s"Expected: ${modifiers.names}")
    }

    lazy val properties = "(" ~> repsep(relation | field, ",") <~ ")"

    val field = ident map FieldProperty
    val relation: Parser[RelationProperty] = ident ~ (modifiers | noModifiers) ~ properties map {
      case name ~ modifiers ~ properties => RelationProperty(name, modifiers, properties: _*)
    }

    def apply(str: String): Either[String, PropertyTree] = parseAll(relation, str) match {
      case Success(tree, _) => Right(tree)
      case NoSuccess(msg, next) => Left(s"$msg (${next.pos})")
    }
  }
}
