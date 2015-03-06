package net.mm.composer

import com.twitter.finatra.{Controller, Request, ResponseBuilder}
import com.twitter.logging.Logger
import com.twitter.util.Future
import net.mm.composer.properties.PropertiesParser
import net.mm.composer.relations.RelationJsonComposer

trait CompositionResponseBuilder {
  self: Controller =>

  private val PropertiesParam = "properties"

  protected def propertiesParser: PropertiesParser

  protected def relationComposer: RelationJsonComposer

  implicit class CompositionSupport(render: ResponseBuilder) {

    def composedJson[T](obj: Any)(implicit request: Request, m: Manifest[T]): Future[ResponseBuilder] = composedJson(obj, m.runtimeClass)(request)

    def composedJson(obj: Any, clazz: Class[_])(implicit request: Request): Future[ResponseBuilder] = {
      request.params.get(PropertiesParam)
        .map(propertiesParser.parse)
        .map{
          case Right(propertyTree) =>
            relationComposer.compose(obj, clazz)(propertyTree)
              .map(render.json)
              .onFailure(Logger.get.warning(_, "Relation composition failed"))
          case Left(error) =>
            render.badRequest.body(error).toFuture
        }
        .getOrElse(render.json(obj).toFuture)
    }
  }

}
