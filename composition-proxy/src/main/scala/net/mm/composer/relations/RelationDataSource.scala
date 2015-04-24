package net.mm.composer.relations

class RelationDataSource private[relations](result: Map[Relation.Source[_, _], collection.Map[_, _]]) {

  def get[T, Id](relation: Relation[_, T, Id], id: Id): Option[T] = {
    result.asInstanceOf[Map[Relation.Source[Id, T], collection.Map[Id, T]]](relation.source).get(id)
  }

  private[relations] def dataMap: Map[Relation.Source[_, _], collection.Map[_, _]] = result
}
