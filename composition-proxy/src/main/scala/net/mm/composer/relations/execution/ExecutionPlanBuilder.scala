package net.mm.composer.relations.execution

import net.mm.composer.properties.{Property, RelationProperty}
import net.mm.composer.relations.Relation.AnyRelation
import net.mm.composer.relations.RelationRegistry
import net.mm.composer.relations.execution.ExecutionHint.NonBijective
import net.mm.composer.utils.SeqTypeFilterSupport._

trait ExecutionPlanBuilder {
  def apply(relationProperties: Seq[RelationProperty], clazz: Class[_]): ExecutionPlan

  def apply(properties: Seq[Property], clazz: Class[_])(implicit d: DummyImplicit): ExecutionPlan

  def apply[T](relationProperties: Seq[RelationProperty])(implicit m: Manifest[T]): ExecutionPlan = apply(relationProperties, m.runtimeClass)

  def apply[T](properties: Seq[Property])(implicit m: Manifest[T], d: DummyImplicit): ExecutionPlan = apply(properties, m.runtimeClass)
}

class ExecutionPlanBuilderImpl(implicit relationRegistry: RelationRegistry) extends ExecutionPlanBuilder {

  private implicit class PreponeSupport(rel: AnyRelation){
    /**
     * @return true if a task can be preponed for execution with it's parent
     */
    def prepone(task: TaskNode): Boolean = !rel.executionHints(NonBijective) && rel.idExtractor == task.relation.idExtractor
  }

  def apply(properties: Seq[Property], clazz: Class[_])(implicit dummyImplicit: DummyImplicit): ExecutionPlan = {
    apply(properties.typeFilter[RelationProperty], clazz)
  }

  def apply(relationProperties: Seq[RelationProperty], clazz: Class[_]): ExecutionPlan = {
    val tempGraph = for {
      relationProperty <- relationProperties
      relation <- relationRegistry.get(clazz, relationProperty.name)
    } yield {
      // build the execution plan buttom up by sorting tasks according to a cost function
      // and preponing tasks to their parent level if possible
      val childRelations = relationProperty.properties.typeFilter[RelationProperty]
      val (preponed, childTasks) = apply(childRelations, relation.target).partition(relation.prepone)
      (preponed :+ new TaskNode(relationProperty.name, relation, childTasks.sortBy(_.costs): _*)).sortBy(_.costs)
    }
    tempGraph.flatten
  }
}
