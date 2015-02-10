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
  def compose(obj: Any, clazz: Class[_])(propertyTree: PropertyTree): Future[JsonNode]
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

  def compose(obj: Any, clazz: Class[_])(propertyTree: PropertyTree): Future[JsonNode] = {
    val partialComposer = executeAndCompose(propertyTree, clazz)_
    obj match {
      case seq: Seq[_] => partialComposer(executionScheduler.run(seq), implicit dataSource => toArrayNode(_,_)(seq))
      case _ => partialComposer(executionScheduler.run(obj), implicit dataSource => toObjectNode(_,_)(obj))
    }
  }


  private def executeAndCompose[T](propertyTree: PropertyTree, clazz: Class[T])(executer: ExecutionPlan => Future[RelationDataSource], composer: RelationDataSource => (Fields, ChildRelations) => JsonNode): Future[JsonNode] = {
    val executionPlan = executionPlanBuilder(propertyTree, clazz)
    executer(executionPlan).map(ds =>
      composer(ds)(propertyTree.childNames, childRelations(propertyTree, clazz))
    )
  }
}
