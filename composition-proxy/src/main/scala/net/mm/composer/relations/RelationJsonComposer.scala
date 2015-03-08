package net.mm.composer.relations

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, NullNode, ObjectNode, ValueNode}
import com.twitter.finatra.serialization.DefaultJacksonJsonSerializer
import com.twitter.logging.Logger
import com.twitter.util.Future
import net.mm.composer.properties.{Property, RelationProperty}
import net.mm.composer.relations.Relation.AnyRelation
import net.mm.composer.relations.execution.SerializationHint.Array
import net.mm.composer.relations.execution.{ExecutionPlanBuilder, ExecutionScheduler}
import net.mm.composer.utils.SeqTypeFilterSupport._

import scala.collection.JavaConversions._
import scala.collection.parallel.ParSeq

trait RelationJsonComposer {
  def compose(obj: Any, clazz: Class[_])(properties: Seq[Property]): Future[JsonNode]
}

class RelationJsonComposerImpl(implicit executionScheduler: ExecutionScheduler, executionPlanBuilder: ExecutionPlanBuilder, registry: RelationRegistry) extends RelationJsonComposer {

  private val mapper = DefaultJacksonJsonSerializer.mapper

  type ChildRelations = ParSeq[(RelationProperty, AnyRelation)]
  type Fields = Seq[String]

  private implicit class LoadedRelation[From, Target, Id](relation: Relation[From, Target, Id])(implicit dataSource: RelationDataSource) {

    def loadTargets(fromObj: From): Iterable[Target] = relation.idExtractor(fromObj).flatMap(id =>
      dataSource.get(relation)(id)
    )

    def loadTarget(fromObj: From): Option[Target] = {
      val target = loadTargets(fromObj)
      if(target.size > 1)
        Logger.get.warning(s"Relation target of size ${target.size} for ${relation.target}: expected Array hint for serialization!")
      target.headOption
    }
  }

  private implicit class RelationJsonNode[From, Id](relation: Relation[From, _, Id])(implicit dataSource: RelationDataSource) {
    /**
     * build the relation node dependent on its type and serialization hints
     * @param fromObj the origin of the relation
     * @param property the relation property defining requested fields and further nested relations
     * @return a JsonNode (NullNode in case the relation is not present for the current fromObj)
     */
    def buildNode[Target](fromObj: From, property: RelationProperty): JsonNode = {
      val fields = property.childNames
      val nestedRelations = loadRelations(property.properties, relation.target)

      relation match {
        case rel: Relation[From, Target, Id] if rel.serializationHints(Array) =>
          toArrayNode(fields, nestedRelations)(rel.loadTargets(fromObj))
        case rel: ToOne[From, Target, Id] =>
          rel.loadTarget(fromObj).fold[JsonNode](NullNode.instance)(toValueOrObjectNode(fields, nestedRelations))
        case rel: ToMany[From, Target, Id] =>
          rel.loadTarget(fromObj).fold[JsonNode](NullNode.instance)(toArrayNode(fields, nestedRelations))
      }
    }
  }

  /**
   * Load relations for properties of clazz
   * @param properties the properties (only relation properties are considered)
   * @param clazz the current clazz
   * @return a ParSeq of relations to facilitate parallel composition
   */
  private def loadRelations(properties: Seq[Property], clazz: Class[_]): ChildRelations = properties
    .typeFilter[RelationProperty]
    .par.flatMap(prop =>
      registry.get(clazz, prop.name).map(rel => (prop, rel))
    )


  private def toArrayNode[From, Id](fields: Fields, relations: ChildRelations)(seq: Iterable[From])(implicit dataSource: RelationDataSource): ArrayNode = {
    val composeEach = toValueOrObjectNode(fields, relations) _
    seq.map(composeEach).foldLeft(mapper.createArrayNode())(_ add _)
  }

  private def toValueOrObjectNode[From, Id](fields: Fields, relations: ChildRelations)(obj: From)(implicit dataSource: RelationDataSource): JsonNode = {
    mapper.valueToTree[JsonNode](obj) match  {
      case objNode: ObjectNode =>
        relations.foreach{ case (property, relation: Relation[From, _, Id]) =>
          objNode.set(property.name, relation.buildNode(obj, property))
        }
        objNode.retain(fields)
      case valNode: ValueNode =>
        if(!fields.isEmpty || !relations.isEmpty)
          Logger.get.warning(s"Unexpected fields $fields or relations $relations for value node of type ${obj.getClass}")
        valNode
    }
  }

  def compose(obj: Any, clazz: Class[_])(properties: Seq[Property]): Future[JsonNode] = {
    val executionPlan = executionPlanBuilder(properties, clazz)

    val fields = properties.map(_.name)
    val relations = loadRelations(properties, clazz)

    obj match {
      case seq: Seq[_] => executionScheduler.run(seq)(executionPlan).map( implicit dataSource =>
        toArrayNode(fields, relations)(seq)
      )
      case _ => executionScheduler.run(obj)(executionPlan).map( implicit dataSource =>
        toValueOrObjectNode(fields, relations)(obj)
      )
    }
  }

}
