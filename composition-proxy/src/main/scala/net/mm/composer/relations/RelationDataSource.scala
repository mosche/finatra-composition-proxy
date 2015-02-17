package net.mm.composer.relations

case class RelationDataSource private[relations](result: Map[Relation.RelationSource[_, _], collection.Map[_, _]]) {
  def get[T, Id](relation: Relation[_, T, Id])(id: Id): Option[T] = {
    result.asInstanceOf[Map[Relation.RelationSource[Id, T], collection.Map[Id, T]]](relation.source).get(id)
  }
}

object RelationDataSource {
  private[relations] def apply(entries: (Relation.RelationSource[_, _], collection.Map[_, _])*): RelationDataSource = {
    new RelationDataSource(entries.toMap)
  }
}
