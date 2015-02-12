package net.mm.composer.relations

import com.twitter.util.Future
import net.mm.composer.relations.Relation._
import net.mm.composer.relations.execution.{ExecutionHint, Hint, SerializationHint}

sealed abstract class Relation[-From, Target, Key](val target: Class[_]) {
  def key: RelationKey[From, Key]
  def source: RelationSource[Key, Target]
  def hints: Seq[Hint]
  val executionHints = hints.filter(_.isInstanceOf[ExecutionHint]).toSet
  val serializationHints = hints.filter(_.isInstanceOf[SerializationHint]).toSet
}

object Relation {
  type AnyRelation = Relation[_, _, _]
  type RelationFor[F] = Relation[F,_,_]
  type RelationWithKey[K] = Relation[_,_,K]

  type RelationSource[Key, Target] = Set[Key] => Future[Map[Key, Target]]

  type RelationKey[From, Key] = From => Iterable[Key]

  object RelationKey {
    def lift[Key](f: PartialFunction[Any, Key]): RelationKey[Any, Key] = from => f.lift(from)
    def apply[Key](f: PartialFunction[Any, Iterable[Key]]): RelationKey[Any, Key] = {
      from => if(f.isDefinedAt(from)) f(from) else Iterable.empty
    }
  }
}

case class ToOne[-From, Target, Key](key: RelationKey[From, Key], source: RelationSource[Key, Target], hints: Hint*)(implicit m: Manifest[Target])
  extends Relation[From, Target, Key](m.runtimeClass)

case class ToMany[-From, Target, Key](key: RelationKey[From, Key], source: RelationSource[Key, Seq[Target]], hints: Hint*)(implicit m: Manifest[Target])
  extends Relation[From, Seq[Target], Key](m.runtimeClass)
