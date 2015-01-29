package net.mm.composer.relations

case class RelationDataSource private[relations](result: Map[Relation.Executor[_, _], collection.Map[_, _]]) {
  def get[T, Id](relation: Relation[_, T, Id])(id: Id): Option[T] = {
    result.asInstanceOf[Map[Relation.Executor[Id, T], collection.Map[Id, T]]](relation.apply).get(id)
  }
}

object RelationDataSource {
  private[relations] def apply(entries: (Relation.Executor[_, _], collection.Map[_, _])*): RelationDataSource = {
    new RelationDataSource(entries.toMap)
  }
}
