package net.mm.composer.relations.execution

import com.twitter.logging.Logger
import com.twitter.util._
import net.mm.composer.relations.{Relation, RelationDataSource, ToMany, ToOne}

import scala.collection.mutable


trait ExecutionScheduler {

  def run(tasks: ExecutionPlan): Runner

  trait Runner {
    /**
     * Run the execution plan on the given input sequence.
     * @return a relation data source containing all results
     */
    def apply(seq: Iterable[Any]): Future[RelationDataSource]
    /**
     * Run the execution plan on the given entity.
     * @return a relation data source containing all results
     */
    def apply(any: Any): Future[RelationDataSource]
  }
}


class ExecutionSchedulerImpl extends ExecutionScheduler {

  private val logger = Logger.get

  def run(tasks: ExecutionPlan): Runner = new ExecutionPlanRunner(tasks) with TimedRunner

  trait TimedRunner extends Runner {
    abstract override def apply(seq: Iterable[Any]): Future[RelationDataSource] = {
      val time = Time.now
      super.apply(seq).onSuccess(_ => logger.ifDebug(s"Loaded relations in ${time.untilNow}"))
    }
  }

  class ExecutionPlanRunner(tasks: ExecutionPlan) extends Runner {

    def apply(any: Any): Future[RelationDataSource] = apply(Seq(any))

    def apply(seq: Iterable[Any]): Future[RelationDataSource] = schedule(tasks, seq).map(_ =>
        new RelationDataSource(executors.mapValues(_.getResult()).toMap)
    )

    private type Executors[Id, T] = mutable.Map[Relation.Source[Id, T], BatchSourceExecutor[Id, T]]
    private val executors:Executors[_,_] = mutable.Map.empty

    implicit private class BatchSource[Id, T](source: Relation.Source[Id, T]){
      def batch: BatchSourceExecutor[Id, T] = executors.synchronized {
        executors.asInstanceOf[Executors[Id,T]].getOrElseUpdate(source, new BatchSourceExecutor(source))
      }
    }

    /**
     * Schedule execution according to the given execution plan depth first in reverse order.
     */
    private def schedule[From, Id](tasks: ExecutionPlan, seq: Iterable[From]): Future[Any] = {
      logger.ifTrace(s"Scheduling tasks ${tasks.names} for ${seq.headOption.map(_.getClass.getSimpleName).getOrElse("Nothing")}")

      // register ids first to allow aggregation of requests
      val idsPerTask = tasks.map(task => (task, registerIds(task.relation.asInstanceOf[Relation[From, _, Id]], seq))).toMap

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

    private def registerIds[From, Id](relation: Relation[From, _, Id], seq: Iterable[From]): Set[Id] = {
      val ids: Set[Id] = seq.flatMap(relation.idExtractor).toSet
      relation.source.batch.addIds(ids)
      ids
    }

    /**
     * Execute a task with its subtasks.
     */
    private def execute[Id, T](task: TaskNode, ids: Set[Id]): Future[Any] = {
      // resolve and descend depth first to load child nodes
      val res = task.relation match {
        case rel: ToOne[_, T, Id] => for {
          result <- rel.source.batch.execute(ids)
          _ <- schedule(task.subTasks, result.values.toSeq)
        } yield ()

        case rel: ToMany[_, T, Id] => for {
          result <- rel.source.batch.execute(ids)
          _ <- schedule(task.subTasks, result.values.flatten.toSeq)
        } yield ()
      }
      res.respond {
        case Return(_) => logger.ifDebug(s"Finished execution plan ${Seq(task).debugString} with $ids")
        case Throw(e) => logger.ifWarning(e, s"Failed to execute ${Seq(task).debugString} with $ids")
      }
    }
  }
}