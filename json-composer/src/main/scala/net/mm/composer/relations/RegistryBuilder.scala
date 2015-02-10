package net.mm.composer.relations

import com.twitter.finatra.{Request, ResponseBuilder}
import com.twitter.util.Future
import net.mm.composer.FinatraResponseComposer
import net.mm.composer.RichRequest._
import net.mm.composer.relations.Relation._
import net.mm.composer.relations.RelationRegistry._
import net.mm.composer.utils.ParamConverter

import scala.collection.mutable.HashMap

class RegistryBuilder{

  private var resources: HashMap[String, Resource] = HashMap.empty

  private var relationsMap: HashMap[Class[_], Relations] = HashMap.empty

  sealed trait Resource{
    type LiftedRender = ResponseBuilder => FinatraResponseComposer#ComposingResponseBuilder
    type Callback = (Request) => Future[ResponseBuilder]

    def apply(render: ResponseBuilder)(implicit lift: LiftedRender): Callback
  }

  class ResourceById[K: ParamConverter](resolver: Executor[K, _], clazz: Class[_]) extends Resource {
    override def apply(render: ResponseBuilder)(implicit lift: LiftedRender): Callback = implicit request => {
      request.getRouteParam("id").fold(render.badRequest.toFuture)( id =>
        resolver(Set(id)).flatMap{
          case res if res.contains(id) => render.composedJson(res(id), clazz)
          case _ => render.notFound.toFuture
        }
      )
    }
  }

  class AsResource[T] private[RegistryBuilder](path: String, target: Class[_]){

    def as[K: ParamConverter](key: RelationKey[_, K], resolver: Executor[K, T]): HavingRelations[K] = {
      resources += s"$path/:id" -> new ResourceById(resolver, target)
      new HavingRelations(key)
    }

    class HavingRelations[K: ParamConverter](key: RelationKey[_, K]){

      def having(relations: (String, RelationFor[T])*): RegistryBuilder = {
        relations.filter(_._2.key == key).foreach{
          case (segment, relation: RelationWithKey[K]) =>
            resources += s"$path/:id/$segment" -> new ResourceById(relation.apply, relation.target)
        }

        relationsMap += target -> relations.toMap
        RegistryBuilder.this
      }
    }
  }
  
  /**
   * Register a relation as resource on a path
   * @param path the path
   * @tparam T the target type
   * @return a resource builder
   */
  def register[T](path: String)(implicit m: Manifest[T]): AsResource[T] = new AsResource(path, m.runtimeClass)

  def build(): RelationRegistry = new RelationRegistry(relationsMap.toMap)
}

object RegistryBuilder {
  def apply(): RegistryBuilder = new RegistryBuilder
}
