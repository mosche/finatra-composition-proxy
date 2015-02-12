package net.mm.composer.relations.execution

import com.twitter.finagle.util.DefaultTimer
import com.twitter.logging.Logger
import com.twitter.util.{Duration, Future, Return, Throw, Time}
import net.mm.composer.relations.Relation.RelationSource

import scala.collection.mutable

class BatchSourceExecutor[Id, Target] private[execution](source: RelationSource[Id, Target]) {
  val log = Logger.get

  private val timeout = Duration.fromMilliseconds(500)

  private[this] var result = Future(mutable.Map.empty[Id, Target])
  private[this] val idsDone = mutable.Set.empty[Id]
  private[this] val idsPlanned = mutable.Set.empty[Id]

  private def timedApply(ids: Set[Id]) = {
    val timer = Time.now
    source(ids)
      .within(DefaultTimer.twitter, timeout)
      .respond(_ => log.ifDebug(s"BatchSource@${source.hashCode} execution done in ${timer.untilNow}, Ids: ${ids.mkString(",")}"))
  }

  def addIds(ids: Set[Id]): Unit = synchronized {
    log.ifTrace(s"BatchSource@${source.hashCode}: adding ids ${ids.mkString(",")}")
    idsPlanned ++= (ids -- idsDone)
  }

  def execute(returnIds: Set[Id]): Future[collection.Map[Id, Target]] = synchronized {
    val ids = idsPlanned.toSet
    idsPlanned.clear()
    idsDone ++= ids

    assert(returnIds.forall(idsDone), "requested execution on ids that were never registered")

    if (!ids.isEmpty) {
      result = Future.join(result, timedApply(ids)).map(p => p._1 ++= p._2)
    }

    result.map(_.filterKeys(returnIds))
  }

  def getResult(): collection.Map[Id, Target] = result.poll match {
    case Some(Return(value)) => value
    case Some(Throw(exception)) => throw exception
    case None => throw new Exception("Attempt to access result before source execution is done!")
  }
}
