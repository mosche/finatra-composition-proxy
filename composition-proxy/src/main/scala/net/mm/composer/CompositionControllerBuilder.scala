package net.mm.composer

import com.twitter.finatra.Controller
import net.mm.composer.properties.PropertiesParser
import net.mm.composer.relations.Relation._
import net.mm.composer.relations.RelationRegistry._
import net.mm.composer.relations.execution.ExecutionScheduler
import net.mm.composer.relations.{Relation, RelationJsonComposer, RelationRegistry}
import net.mm.composer.utils.ParamConverter

import scala.collection.mutable.HashMap

class CompositionControllerBuilder{

  private val resources: HashMap[String, CompositionResource] = HashMap.empty

  private val relationsMap: HashMap[Class[_], Relations] = HashMap.empty

  class AsResource[T] private[CompositionControllerBuilder](path: String, target: Class[_]){

    def as[Id: ParamConverter](idExtractor: IdExtractor[_, Id], relationSource: Source[Id, T]): HavingRelations[Id] = {
      resources += s"$path/:id" -> new ResourceById(relationSource, target)
      new HavingRelations(idExtractor)
    }

    class HavingRelations[Id: ParamConverter](idExtractor: IdExtractor[_, Id]){

      def having(relations: (String, Relation[T, _ , _])*): CompositionControllerBuilder = {
        relations.filter(_._2.idExtractor == idExtractor).foreach{
          case (segment, relation: Relation[T, _, Id]) =>
            resources += s"$path/:id/$segment" -> new ResourceById(relation.source, relation.target)
        }

        relationsMap += target -> relations.toMap
        CompositionControllerBuilder.this
      }
    }
  }
  
  /**
   * Register a resource
   * @param path the resource path
   * @tparam T the target type
   * @return a resource builder
   */
  def register[T](path: String)(implicit m: Manifest[T]): AsResource[T] = new AsResource(path.stripMargin('/'), m.runtimeClass)

  /**
   * Build a composition controller based on the registered resources
   * Implicit dependencies required on a PropertiesParser, a ExecutionScheduler
   * and a RelationJsonComposer factory
   *
   * @param pathPrefix global path prefix for this controller (default: empty)
   * @return the controller
   */
  def buildController(pathPrefix: String = "")
     (implicit executionScheduler: ExecutionScheduler,
      propertiesParserFactory: RelationRegistry => Class[_] => PropertiesParser,
      relationComposerFactory: RelationRegistry => RelationJsonComposer): Controller = {

    def path(segments: String):String = if(pathPrefix.isEmpty) "/"+segments else s"/${pathPrefix.stripMargin('/')}/$segments"

    val relationRegistry = buildRegistry()
    implicit val relationComposer = relationComposerFactory(relationRegistry)
    implicit val propertiesParser = propertiesParserFactory(relationRegistry)

    new CompositionController{
      resources.mapValues(_.apply(() => render)).foreach{
        // add GET route for each registered resource
        case (segments, resource) => get(path(segments))(resource)
      }

      def registeredResources = resources.keys.toSeq.sorted.foldLeft("")(_ + "GET " + path(_) + "\n")
      log.info(s"Registered composition resources:\n${"*" * 30}\n$registeredResources${"*" * 30}")
    }
  }

  /**
   * Build a relation registry based on the registered relations
   */
  def buildRegistry() = new RelationRegistry(relationsMap.toMap)
}

object CompositionControllerBuilder {
  def apply(): CompositionControllerBuilder = new CompositionControllerBuilder
}
