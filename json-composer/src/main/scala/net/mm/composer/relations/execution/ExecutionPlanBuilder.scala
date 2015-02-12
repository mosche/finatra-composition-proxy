package net.mm.composer.relations.execution

import net.mm.composer.properties.{RelationProperty}
import net.mm.composer.relations.Relation.AnyRelation
import net.mm.composer.relations.RelationRegistry
import net.mm.composer.relations.execution.ExecutionHint.NonBijective

trait ExecutionPlanBuilder {
  def apply[T](property: RelationProperty)(implicit m: Manifest[T]): ExecutionPlan

  def apply(property: RelationProperty, clazz: Class[_]): ExecutionPlan
}

class ExecutionPlanBuilderImpl(implicit relationRegistry: RelationRegistry) extends ExecutionPlanBuilder {

  private implicit class PreponeSupport(rel: AnyRelation){
    /**
     * @return true if a task can be preponed for execution with it's parent
     */
    def prepone(task: TaskNode): Boolean = !rel.executionHints(NonBijective) && rel.key == task.relation.key
  }

  def apply[T](property: RelationProperty)(implicit m: Manifest[T]) = {
    apply(property, m.runtimeClass)
  }

  def apply(property: RelationProperty, clazz: Class[_]): ExecutionPlan = {
    val tempGraph = for {
      tree <- property.childRelations
      relation <- relationRegistry.get(clazz, tree.name)
    } yield {
      // build the execution plan buttom up by sorting tasks according to a cost function
      // and preponing tasks to their parent level if possible
      val (preponed, childTasks) = apply(tree, relation.target).partition(relation.prepone)
      (preponed :+ new TaskNode(tree.name, relation, childTasks.sortBy(_.costs): _*)).sortBy(_.costs)
    }
    tempGraph.flatten
  }
}
