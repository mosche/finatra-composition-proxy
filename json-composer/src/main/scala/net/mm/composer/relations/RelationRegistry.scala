package net.mm.composer.relations

import com.twitter.logging.Logger
import net.mm.composer.relations.Relation.AnyRelation
import net.mm.composer.relations.RelationRegistry._

class RelationRegistry private[relations](registry: Map[Class[_], Relations]) {

  def get(clazz: Class[_], relation: String): Option[AnyRelation] = {
    val rel = registry.get(clazz).flatMap(relations => relations.get(relation))
    Logger.get.ifTrace(s"Loaded relation $relation for ${clazz.getSimpleName}: $rel")
    rel
  }
}

object RelationRegistry {
  type Relations = Map[String, AnyRelation]
}