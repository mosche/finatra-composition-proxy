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

    private type Executors[Id, T] = mutable.Map[Relation.Source[Id, T], BatchSourceExecutor[Id, T]]
    private val executors: Executors[_, _] = mutable.Map.empty

    def apply(any: Any): Future[RelationDataSource] = apply(Seq(any))

    def apply(seq: Iterable[Any]): Future[RelationDataSource] = schedule(tasks, seq).map(_ =>
      new RelationDataSource(executors.mapValues(_.getResult()).toMap)
    )

    /**
     * Schedule execution according to the given execution plan depth first in reverse order.
     */
    private def schedule[From, Id](tasks: ExecutionPlan, seq: Iterable[From]): Future[Any] = {
      logger.ifTrace(s"Scheduling tasks ${tasks.names} for ${seq.headOption.map(_.getClass.getSimpleName).getOrElse("Nothing")}")

      // extract ids left to right and memorize to allow aggregation of requests
      val idsPerTask = tasks.foldLeft(mutable.Map.empty[TaskNode, Set[Id]]){ (idsMap, task) =>
        val relation = task.relation.asInstanceOf[Relation[From, _, Id]]
        val ids = seq.flatMap(relation.idExtractor).toSet
        // SIDE EFFECT: add to batch source for later processing
        relation.source.batch.addIds(ids)
        idsMap += task -> ids
      }

      // execute in reverse order (from right) and fork independent executions
      val executionNodes = tasks.foldRight(Seq.empty[ExecutionNode]) { (currentTask, executionNodes) =>
        val (dependent, independent) = executionNodes.partition(_.tasks.exists(_ dependsOn currentTask))
        if (dependent.isEmpty) {
          independent.forkExecution(currentTask, idsPerTask(currentTask))
        } else {
          dependent.joinExecution(currentTask, idsPerTask(currentTask), independent)
        }
      }
      // join all independent execution nodes
      executionNodes.join()
    }

    /**
     * Execute a task with its subtasks
     */
    private def execute[Id, T](task: TaskNode, ids: Set[Id]): Future[Any] = {
      // resolve and descend depth first to load child nodes
      val res = task.relation match {
        case rel: ToOne[_, T, Id] => for {
          result <- rel.source.batch.execute(ids)
          _ <- schedule(task.subTasks, result.values)
        } yield ()

        case rel: ToMany[_, T, Id] => for {
          result <- rel.source.batch.execute(ids)
          _ <- schedule(task.subTasks, result.values.flatten)
        } yield ()
      }
      res.respond {
        case Return(_) => logger.ifDebug(s"Finished execution plan ${Seq(task).debugString} with $ids")
        case Throw(e) => logger.ifWarning(e, s"Failed to execute ${Seq(task).debugString} with $ids")
      }
    }

    implicit private class BatchSource[Id, T](source: Relation.Source[Id, T]) {
      def batch: BatchSourceExecutor[Id, T] = executors.synchronized {
        executors.asInstanceOf[Executors[Id, T]].getOrElseUpdate(source, new BatchSourceExecutor(source))
      }
    }

    implicit private class ExecutionNodes(nodes: Seq[ExecutionNode]){
      /**
       * Add a new execution node for the given task
       */
      def forkExecution[Id](task: TaskNode, ids: Set[Id]): Seq[ExecutionNode] = {
        logger.ifTrace(s"Forking execution for task ${task.name}")
        ExecutionNode(
          tasks = Set(task),
          future = execute(task, ids)
        ) +: nodes
      }

      /**
       * Build a new execution node by joining the nodes of this followed by the execution of the given task.
       * Independent nodes are simply appended.
       */
      def joinExecution[Id](task: TaskNode, ids: Set[Id], independent: Seq[ExecutionNode]): Seq[ExecutionNode] = {
        val dependentTasks = nodes.flatMap(_.tasks).toSet
        logger.ifTrace(s"Joining execution for task ${task.name} due to ${dependentTasks.names}")
        ExecutionNode(
          tasks = dependentTasks + task,
          future = join().flatMap(_ => execute(task, ids))
        ) +: independent
      }

      /**
       * Join all async independent execution nodes
       */
      def join(): Future[_] = Future.join(nodes.map(_.future))
    }
  }

  private case class ExecutionNode(tasks: Set[TaskNode], future: Future[_])
}