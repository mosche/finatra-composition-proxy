package net.mm.composer.relations

import net.mm.composer.relations.RelationRegistry._

class RegistryBuilder {

  private var relationMap: Map[Class[_], Relations] = Map.empty

  def register[From](relations: (String, Relation[From, _, _])*)(implicit m: Manifest[From]): this.type = synchronized {
    relationMap = relationMap + (m.runtimeClass -> relations.toMap)
    this
  }

  def build(): RelationRegistry = new RelationRegistry(relationMap)
}

object RegistryBuilder {
  def apply(): RegistryBuilder = new RegistryBuilder
}
