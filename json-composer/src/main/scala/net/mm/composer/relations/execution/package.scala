package net.mm.composer.relations

import net.mm.composer.relations.Relation._

package object execution {

  type ExecutionPlan = Seq[TaskNode]

  case class TaskNode(val name: String, val relation: AnyRelation, val subTasks: TaskNode*) {
    val costs: Int = 1 + subTasks.map(_.costs).sum

    private[TaskNode] val executorSet: Set[AnyExecutor] = subTasks.flatMap(_.executorSet).toSet + relation.apply

    def dependsOn(other: TaskNode): Boolean = (executorSet & other.executorSet).isEmpty == false
  }

  implicit class RichExecutionPlan(plan: Iterable[TaskNode]) {
    def names: String = if (plan.isEmpty) "None" else plan.map(_.name).mkString(",")

    def debugString: String = buildString(plan, 0, new StringBuilder).toString()

    private def buildString(graph: Iterable[TaskNode], level: Int, builder: StringBuilder): StringBuilder = graph.foldLeft(builder) {
      case (builder, task) =>
        if (!builder.isEmpty) builder.append('\n')
        builder.append(" " * level)
          .append("Task(").append(task.relation.target.getSimpleName)
          .append(" as ").append(task.name).append(")")

        if (!task.subTasks.isEmpty) {
          builder.append("[")
          buildString(task.subTasks, level + 1, builder)
          builder.append('\n').append(" " * level).append(']')
        }
        builder
    }
  }

}
