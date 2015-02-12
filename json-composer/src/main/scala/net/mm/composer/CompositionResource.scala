package net.mm.composer

import com.twitter.finatra.{Request, ResponseBuilder}
import com.twitter.util.Future
import net.mm.composer.relations.Relation._
import net.mm.composer.utils.ParamConverter
import net.mm.composer.RichRequest._

sealed trait CompositionResource{
  type AsComposingBuilder = ResponseBuilder => CompositionResponseBuilder#CompositionSupport
  type Callback = (Request) => Future[ResponseBuilder]

  def apply(render: ResponseBuilder)(implicit f: AsComposingBuilder): Callback
}

class ResourceById[K: ParamConverter](relationSource: RelationSource[K, _], clazz: Class[_]) extends CompositionResource {
  override def apply(render: ResponseBuilder)(implicit f: AsComposingBuilder): Callback = implicit request => {
    request.getRouteParam("id").fold(render.badRequest.toFuture) { id =>
      println(request)
      relationSource(Set(id)).flatMap {
        case res if res.contains(id) => render.composedJson(res(id), clazz)
        case _ => render.notFound.toFuture
      }
    }
  }
}

class ResourceByIds[K: ParamConverter](relationSource: RelationSource[K, _], clazz: Class[_]) extends CompositionResource {
  def apply(render: ResponseBuilder)(implicit f: AsComposingBuilder): Callback  = implicit request => {
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