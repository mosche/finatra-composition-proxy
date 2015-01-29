package net.mm.composer

package object properties {
  type PropertyTree = RelationProperty

  @deprecated("not implemented yet for the execution scheduler")
  case class Modifier[T](name: String)(implicit val manifest: Manifest[T])

  sealed trait Property {
    def name: String
  }

  case class FieldProperty(name: String) extends Property

  case class RelationProperty(name: String, modifiers: Map[String, Any], properties: Property*) extends Property {
    def childRelations: Seq[RelationProperty] = properties.filter(_.isInstanceOf[RelationProperty]).asInstanceOf[Seq[RelationProperty]]
    def childNames: Seq[String] = properties.map(_.name)
  }

  object RelationProperty {
    def apply(name: String, properties: Property*): RelationProperty = new RelationProperty(name, Map.empty, properties: _*)
  }

}
