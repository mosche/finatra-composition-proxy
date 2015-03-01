package net.mm.composer.relations

import com.twitter.util.Future
import net.mm.composer.relations.Relation._
import net.mm.composer.relations.execution.{ExecutionHint, Hint, SerializationHint}

sealed abstract class Relation[-From, Target, Id](val target: Class[_]) {
  def idExtractor: IdExtractor[From, Id]
  def source: Source[Id, Target]
  def hints: Seq[Hint]
  val executionHints = hints.filter(_.isInstanceOf[ExecutionHint]).toSet
  val serializationHints = hints.filter(_.isInstanceOf[SerializationHint]).toSet
}

object Relation {
  type AnyRelation = Relation[_, _, _]

  type Source[Id, Target] = Set[Id] => Future[Map[Id, Target]]

  type IdExtractor[From, Id] = From => Iterable[Id]

  object IdExtractor {
    def lift[Id](f: PartialFunction[Any, Id]): IdExtractor[Any, Id] = from => f.lift(from)
    def apply[Id](f: PartialFunction[Any, Iterable[Id]]): IdExtractor[Any, Id] = {
      from => if(f.isDefinedAt(from)) f(from) else Iterable.empty
    }
  }
}

case class ToOne[-From, Target, Id](idExtractor: IdExtractor[From, Id], source: Source[Id, Target], hints: Hint*)(implicit m: Manifest[Target])
  extends Relation[From, Target, Id](m.runtimeClass)

case class ToMany[-From, Target, Id](idExtractor: IdExtractor[From, Id], source: Source[Id, Seq[Target]], hints: Hint*)(implicit m: Manifest[Target])
  extends Relation[From, Seq[Target], Id](m.runtimeClass)
