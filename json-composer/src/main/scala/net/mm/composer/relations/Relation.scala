package net.mm.composer.relations

import com.twitter.util.Future
import net.mm.composer.relations.Relation._
import net.mm.composer.relations.execution.{ExecutionHint, Hint, SerializationHint}

sealed abstract class Relation[-From, Target, Key] {
  def key: RelationKey[From, Key]
  def apply: Executor[Key, Target]
  def target: Class[_]
  def hints: Seq[Hint]
  val executionHints = hints.filter(_.isInstanceOf[ExecutionHint]).toSet
  val serializationHints = hints.filter(_.isInstanceOf[SerializationHint]).toSet
}

object Relation {
  type AnyRelation = Relation[_, _, _]
  type AnyExecutor = Executor[_, _]
  type Executor[Key, Target] = Set[Key] => Future[Map[Key, Target]]

  type RelationKey[From, Key] = From => Iterable[Key]

  object RelationKey {
    def lift[Key](f: PartialFunction[Any, Key]): RelationKey[Any, Key] = from => f.lift(from)
    def apply[Key](f: PartialFunction[Any, Iterable[Key]]): RelationKey[Any, Key] = {
      from => if(f.isDefinedAt(from)) f(from) else {
        println(s"failed to get key for $from, ${from.getClass}")
        Iterable.empty
      }
    }
  }
}

case class ToOne[-From, Target, Key](key: RelationKey[From, Key], apply: Executor[Key, Target], hints: Hint*)(implicit m: Manifest[Target])
  extends Relation[From, Target, Key] {

  def target = m.runtimeClass
  //override def toString = s"ToOne(Key@${key.hashCode},Executor@${apply.hashCode})"
}

case class ToMany[-From, Target, Key](key: RelationKey[From, Key], apply: Executor[Key, Seq[Target]], hints: Hint*)(implicit m: Manifest[Seq[Target]])
  extends Relation[From, Seq[Target], Key] {

  def target = m.typeArguments.head.runtimeClass
  //override def toString = s"ToMany(Key@${key.hashCode},Executor@${apply.hashCode})"
}
