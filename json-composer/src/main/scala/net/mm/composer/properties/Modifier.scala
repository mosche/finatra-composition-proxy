package net.mm.composer.properties


@deprecated("not implemented yet for the execution scheduler")
case class Modifier[T: TokenParser.Parser](name: String)(implicit m: Manifest[T]){
  val valueType = m.runtimeClass.getSimpleName
  def parser: TokenParser.Parser[T] = implicitly[TokenParser.Parser[T]]
}

object Modifier{
  import TokenParser._

  implicit val intParser: TokenParser.Parser[Int] = "\\d+".r map (_.toInt)
  implicit val stringParser: TokenParser.Parser[String] = ident | stringLiteral
}