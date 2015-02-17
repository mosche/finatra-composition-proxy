package net.mm.composer

import com.twitter.finatra.Request
import net.mm.composer.utils.ParamConverter

trait RichRequest {

  implicit class ParamConversionSupport(request: Request) {

    def getRouteParam[T: ParamConverter](name: String): Option[T] = {
      request.routeParams.get(name).flatMap(implicitly[ParamConverter[T]].apply)
    }

    def getRequestParam[T: ParamConverter](name: String): Option[T] = {
      Option(request.getParam(name)).flatMap(implicitly[ParamConverter[T]].apply)
    }
  }

}

object RichRequest extends RichRequest
