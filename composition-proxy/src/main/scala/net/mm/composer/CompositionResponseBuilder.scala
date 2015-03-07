package net.mm.composer

import com.twitter.finatra.{Controller, ResponseBuilder}
import com.twitter.logging.Logger
import com.twitter.util.Future
import net.mm.composer.properties.Property
import net.mm.composer.relations.RelationJsonComposer

trait CompositionResponseBuilder {
  self: Controller =>

  protected def relationComposer: RelationJsonComposer

  implicit class CompositionSupport(render: ResponseBuilder) {

    def composedJson[T](obj: Any)(implicit properties: Seq[Property], m: Manifest[T]): Future[ResponseBuilder] = composedJson(obj, m.runtimeClass)

    def composedJson(obj: Any, clazz: Class[_])(implicit properties: Seq[Property]): Future[ResponseBuilder] = {
      relationComposer.compose(obj, clazz)(properties)
        .map(render.json)
        .onFailure(Logger.get.warning(_, "Relation composition failed"))
    }
  }

}
