package net.mm.composer.relations

import net.mm.composer.relations.Relation
import net.mm.composer.relations.Relation.AnyRelation
import net.mm.composer.relations.RelationRegistry._

class RegistryBuilder {

  private var relationsMap: Map[Class[_], Relations] = Map.empty

  private var rootRelations: Map[String, AnyRelation] = Map.empty

  /**
   * Builder for child relations
   * @param clazz actually Class[From]
   * @tparam From the root relation target type
   */
  class RelationsBuilder[From] private[RegistryBuilder] (clazz: Class[_]){

    /**
     * Configure relations of a root relation
     * @param relations as a pair (name -> relation) each
     * @return the registry builder
     */
    def having(relations: (String, Relation[From, _, _])*): RegistryBuilder = {
      RelationsBuilder.this.synchronized {
        relationsMap = relationsMap + (clazz -> relations.toMap)
        RegistryBuilder.this
      }
    }
  }

  /**
   * Register a root relation with a name
   * @param rootRelation as a pair (name -> relation)
   * @tparam T the target type
   * @return a relations builder to configure child relations
   */
  def register[T](rootRelation: (String, Relation[_, T, _]))(implicit m: Manifest[T]): RelationsBuilder[T] = {
    synchronized {
      rootRelations = rootRelations + rootRelation
      new RelationsBuilder(m.runtimeClass)
    }
  }

  def build(): RelationRegistry = new RelationRegistry(relationsMap)
}

object RegistryBuilder {
  def apply(): RegistryBuilder = new RegistryBuilder
}
