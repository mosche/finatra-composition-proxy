package net.mm.composer.properties

sealed trait Property {
  def name: String
  def description: String
}

case class FieldProperty(name: String) extends Property{
  def description: String = s"field '$name'"
}

case class RelationProperty(name: String, modifiers: Map[String, Any], properties: Seq[Property]) extends Property {
  def childNames: Seq[String] = properties.map(_.name)
  def description: String = s"relation '$name'"
}

object RelationProperty {
  def apply(name: String, properties: Property*): RelationProperty = new RelationProperty(name, Map.empty, properties)
}