package net.mm.composer.properties

import net.mm.composer.relations.RelationRegistry

/**
 * Rewrite field properties as relation properties
 * if an according relation is registered
 */
class FieldPropertyRewriterImpl(clazz: Class[_])(implicit relationRegistry: RelationRegistry) extends PropertiesReader {

  private def rewrite(properties: Seq[Property], clazz: Class[_]): Seq[Property] = properties
    .map(p => (p, relationRegistry.get(clazz, p.name)))
    .map{
      case (property, None) => property
      case (property: FieldProperty, Some(_)) => RelationProperty(property.name)
      case (property: RelationProperty, Some(relation)) => property.copy(properties = rewrite(property.properties, relation.target))
    }

  override def apply(parsedProperties: Either[Error, Seq[Property]]): Either[Error, Seq[Property]] = {
    parsedProperties.right.map(rewrite(_,clazz))
  }
}
