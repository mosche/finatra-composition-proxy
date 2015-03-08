package net.mm.composer

import com.twitter.finatra.{Request, ResponseBuilder}
import com.twitter.util.Future
import net.mm.composer.RichRequest._
import net.mm.composer.properties.PropertiesParser
import net.mm.composer.properties.Property
import net.mm.composer.relations.Relation._
import net.mm.composer.utils.ParamConverter

sealed trait CompositionResource{
  type AsComposingBuilder = ResponseBuilder => CompositionResponseBuilder#CompositionSupport
  type ResourceCallback = (Request) => Future[ResponseBuilder]

  protected val PropertiesParam = "properties"

  def apply(render: () => ResponseBuilder)(implicit propertiesParser: Class[_] => PropertiesParser,builder: AsComposingBuilder): ResourceCallback
}

class ResourceById[K: ParamConverter](relationSource: Source[K, _], clazz: Class[_]) extends CompositionResource {

  private def loadFromSource(id: K) = relationSource(Set(id)).map(res => res.get(id))

  override def apply(render: () => ResponseBuilder)(implicit parser: Class[_] => PropertiesParser, builder: AsComposingBuilder): ResourceCallback = {
    val propertiesParser = parser(clazz)
    
    implicit request => request.getRouteParam("id")
      .fold(render().badRequest.toFuture){ id =>

        def withoutProperties = loadFromSource(id)
          .map(_.fold(render().notFound)(render().json))

        def withProperties(properties: Either[String, Seq[Property]]) = properties
          .left.map(error => render().badRequest.body(error).toFuture)
          .right.map(implicit properties => loadFromSource(id).flatMap {
            case Some(obj) => render().composedJson(obj, clazz)
            case None => render().notFound.toFuture
          }).fold(identity, identity)

        request.params.get(PropertiesParam)
          .map(propertiesParser)
          .fold(withoutProperties)(withProperties)
      }
  }
}

class ResourceByIds[K: ParamConverter](relationSource: Source[K, _], clazz: Class[_]) extends CompositionResource {
  def apply(render: () => ResponseBuilder)(implicit parser: Class[_] => PropertiesParser, builder: AsComposingBuilder): ResourceCallback  = implicit request => {
    ???
    /*request.getRequestParam[Set[K]]("ids") match {
      case None =>
        render.badRequest.toFuture
      case Some(ids) =>
        relationSource(ids).flatMap(result =>
          render.composedJson[T](ids.toSeq.flatMap(result.get))
        )
    }*/
  }
}