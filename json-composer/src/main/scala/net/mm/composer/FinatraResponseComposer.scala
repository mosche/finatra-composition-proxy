package net.mm.composer

import com.fasterxml.jackson.databind.JsonNode
import com.twitter.finatra.{Controller, Request, ResponseBuilder}
import com.twitter.util.Future
import net.mm.composer.properties.{PropertiesParser, RelationProperty}
import net.mm.composer.relations.RelationJsonComposer

trait FinatraResponseComposer {
  self: Controller =>

  val PropertiesParam = "properties"

  def propertiesParser: PropertiesParser

  def relationComposer: RelationJsonComposer

  implicit class ComposingResponseBuilder(render: ResponseBuilder) {

    private def renderComposed(properties: String, noRelations: => Any, composedRelations: RelationProperty => Future[JsonNode]): Future[ResponseBuilder] = {
      propertiesParser(properties) match {
        case Right(propertyTree) =>
          composedRelations(propertyTree)
            .map(render.json)
            .onFailure(log.warning(_, "Relation composition failed"))
        case Left(error) =>
          render.badRequest.body(error).toFuture
      }
    }

    def composedJson[T](seq: Seq[T])(implicit request: Request, m: Manifest[T]): Future[ResponseBuilder] = {
      request.params.get(PropertiesParam).fold(render.json(seq).toFuture)(
        properties => renderComposed(properties, seq, relationComposer.compose(seq))
      )
    }

    def composedJson[T](obj: T)(implicit request: Request, m: Manifest[T]): Future[ResponseBuilder] = {
      request.params.get(PropertiesParam).fold(render.json(obj).toFuture)(
        properties => renderComposed(properties, obj, relationComposer.compose(obj))
      )
    }
  }

}
