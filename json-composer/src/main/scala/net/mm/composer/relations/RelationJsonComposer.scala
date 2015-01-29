package net.mm.composer.relations

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}
import com.twitter.finatra.serialization.DefaultJacksonJsonSerializer
import com.twitter.logging.Logger
import com.twitter.util.{Try, Future}
import net.mm.composer.properties.PropertyTree
import net.mm.composer.relations.Relation.AnyRelation
import net.mm.composer.relations.execution.SerializationHint.Array
import net.mm.composer.relations.execution.{ExecutionPlan, ExecutionPlanBuilder, ExecutionScheduler}

import scala.collection.JavaConversions._
import scala.collection.parallel.ParSeq

trait RelationJsonComposer {
  def compose[T](obj: T)(propertyTree: PropertyTree)(implicit m: Manifest[T]): Future[JsonNode]

  def compose[T](seq: Seq[T])(propertyTree: PropertyTree)(implicit m: Manifest[T]): Future[JsonNode]
}

class RelationJsonComposerImpl(implicit executionScheduler: ExecutionScheduler, executionPlanBuilder: ExecutionPlanBuilder, registry: RelationRegistry) extends RelationJsonComposer {

  private val mapper = DefaultJacksonJsonSerializer.mapper

  type ChildRelations = ParSeq[(PropertyTree, AnyRelation)]
  type Fields = Seq[String]

  private implicit class LoadedRelation[From, Target, Id](relation: Relation[From, Target,Id])(implicit ds: RelationDataSource) {

    def appendTargetNode(objNode: ObjectNode, obj: From, property: PropertyTree)(executer: (Fields, ChildRelations) => Target => JsonNode): Unit = {
      val target = relation.key(obj).flatMap(id => ds.get(relation)(id))
      val relations = childRelations(property,relation.target)

      if(relation.serializationHints(Array)){
        objNode.set(property.name, toArrayNode(property.childNames, relations)(target))
      } else {
        if(target.size > 1)
          Logger.get.warning(s"Relation target of size ${target.size} for ${property.name}: expected Array hint for serialization!")
        target.headOption
          .map(executer(property.childNames, relations))
          .map(objNode.set(property.name, _))
      }
    }
  }

  private def childRelations(property: PropertyTree, clazz: Class[_]) = property.childRelations.par.flatMap(rel => registry.get(clazz, rel.name).map((rel, _)))

  private def toArrayNode[T, Id](fields: Fields, relations: ChildRelations)(seq: Iterable[T])(implicit dataSource: RelationDataSource): ArrayNode = {
    def composeEach = toObjectNode(fields, relations) _

    seq.map(composeEach).foldLeft(mapper.createArrayNode())(_ add _)
  }

  private def toObjectNode[From, Id](fields: Fields, relations: ChildRelations)(obj: From)(implicit dataSource: RelationDataSource): ObjectNode = {
    val objNode = mapper.valueToTree[ObjectNode](obj).retain(fields)

    relations.foreach{
      case (property, rel: ToMany[From, _, Id]) =>
        rel.appendTargetNode(objNode, obj, property)(toArrayNode)
      case (property, rel: ToOne[From, _, Id]) =>
        rel.appendTargetNode(objNode, obj, property)(toObjectNode)
    }

    objNode
  }

  private def executeAndCompose[T](propertyTree: PropertyTree, m: Manifest[T])(executer: ExecutionPlan => Future[RelationDataSource], composer: RelationDataSource => (Fields, ChildRelations) => JsonNode): Future[JsonNode] = {
    val executionPlan = executionPlanBuilder(propertyTree, m.runtimeClass)
    executer(executionPlan).map(ds =>
      composer(ds)(propertyTree.childNames, childRelations(propertyTree, m.runtimeClass))
    )
  }

  def compose[T](obj: T)(propertyTree: PropertyTree)(implicit m: Manifest[T]): Future[JsonNode] = {
    executeAndCompose(propertyTree, m)(
      executionScheduler.run(obj),
      implicit dataSource => toObjectNode(_, _)(obj)
    )
  }

  def compose[T](seq: Seq[T])(propertyTree: PropertyTree)(implicit m: Manifest[T]): Future[JsonNode] = {
    executeAndCompose(propertyTree, m)(
      executionScheduler.run(seq),
      implicit dataSource => toArrayNode(_, _)(seq)
    )
  }

}
