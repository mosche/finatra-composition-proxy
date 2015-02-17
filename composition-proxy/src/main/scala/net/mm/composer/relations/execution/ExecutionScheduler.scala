package net.mm.composer.relations.execution

import com.twitter.logging.Logger
import com.twitter.util._
import net.mm.composer.relations.Relation.AnyRelation
import net.mm.composer.relations.{Relation, RelationDataSource, ToMany, ToOne}

import scala.collection.mutable


trait ExecutionScheduler {
  /**
   * Run the execution plan on the given input sequence.
   * @return a relation data source containing all results
   */
  def run(seq: Seq[Any])(tasks: ExecutionPlan): Future[RelationDataSource]

  /**
   * Run the execution plan on the given entity.
   * @return a relation data source containing all results
   */
  def run(entity: Any)(tasks: ExecutionPlan): Future[RelationDataSource]
}


class ExecutionSchedulerImpl extends ExecutionScheduler {
  type Executors = mutable.Map[Relation.RelationSource[_, _], BatchSourceExecutor[_, _]]

  private val logger = Logger.get

  def run(entity: Any)(tasks: ExecutionPlan): Future[RelationDataSource] = run(Seq(entity))(tasks)

  def run(seq: Seq[Any])(tasks: ExecutionPlan): Future[RelationDataSource] = {
    val time = Time.now
    val executors: Executors = mutable.Map.empty
    schedule(tasks, seq)(executors) map { _ =>
      logger.ifDebug(s"Loaded relations in ${time.untilNow}")
      new RelationDataSource(executors.mapValues(_.getResult()).toMap)
    }
  }

  private def getExecutor[Id, T](executor: Relation.RelationSource[Id, T])(implicit executors: Executors): BatchSourceExecutor[Id, T] = executors.synchronized {
    executors
      .asInstanceOf[mutable.Map[Relation.RelationSource[Id, T], BatchSourceExecutor[Id, T]]]
      .getOrElseUpdate(executor, new BatchSourceExecutor(executor))
  }

  /**
   * Schedule execution according to the given execution plan depth first in reverse order.
   */
  private def schedule[From](tasks: ExecutionPlan, seq: Seq[From])(implicit executors: Executors): Future[Any] = {
    logger.ifTrace(s"Scheduling tasks ${tasks.names} for ${seq.headOption.map(_.getClass.getSimpleName).getOrElse("Nothing")}")

    // register ids first to allow aggregation of requests
    val idsPerTask = tasks.map(task => (task, registerIds(task.relation, seq))).toMap

    // execute in reverse order (from right) and fork independent executions
    val forkedExecution = tasks.foldRight[Seq[(Set[TaskNode], Future[_])]](Seq.empty) {
      case (currentTask, taskAkk) =>
        val (dependent, independent) = taskAkk.partition(_._1.exists(_ dependsOn currentTask))
        if (dependent.isEmpty) {
          // fork execution
          logger.ifTrace(s"Forking execution for task ${currentTask.name}")
          (Set(currentTask), execute(currentTask, idsPerTask(currentTask))) +: independent
        } else {
          // join execution on dependent plans
          val dependentTasks = dependent.flatMap(_._1).toSet
          logger.ifTrace(s"Joining execution for task ${currentTask.name} due to ${dependentTasks.names}")
          val joinedFuture = Future.join(dependent.map(_._2)).flatMap(_ => execute(currentTask, idsPerTask(currentTask)))
          (dependentTasks, joinedFuture) +: independent
        }
    }
    // join all independent executions
    Future.join(forkedExecution.map(_._2))
  }

  private def registerIds[From, Id](relation: AnyRelation, seq: Seq[From])(implicit executors: Executors): Set[Id] = {
    val ids: Set[Id] = relation match {
      case rel: ToOne[From, _, Id] => seq.flatMap(rel.key(_)).toSet
      case rel: ToMany[From, _, Id] => seq.flatMap(rel.key(_)).toSet
    }

    getExecutor(relation.source).asInstanceOf[BatchSourceExecutor[Id, _]].addIds(ids)
    ids
  }

  /**
   * Execute a task with its subtasks.
   */
  private def execute[Id, T](task: TaskNode, ids: Set[Id])(implicit executors: Executors): Future[Any] = {
    // resolve and descend depth first to load child nodes
    val res = task.relation match {
      case rel: ToOne[_, T, Id] => for {
        result <- getExecutor(rel.source).execute(ids)
        _ <- schedule(task.subTasks, result.values.toSeq)
      } yield ()

      case rel: ToMany[_, T, Id] => for {
        result <- getExecutor(rel.source).execute(ids)
        _ <- schedule(task.subTasks, result.values.flatten.toSeq)
      } yield ()
    }
    res.respond {
      case Return(_) => logger.ifDebug(s"Finished execution plan ${Seq(task).debugString} with $ids")
      case Throw(e) => logger.ifWarning(e, s"Failed to execute ${Seq(task).debugString} with $ids")

    }
  }
}