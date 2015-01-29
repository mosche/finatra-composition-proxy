package net.mm.composer

import java.util.UUID

import scala.collection.mutable
import scala.util.control.Exception._

trait RouteParamsSupport {

  implicit class RouteParamsConversion(params: mutable.Map[String, String]) {

    def getUUID(key: String): Option[UUID] = getAs(key, UUID.fromString)

    def getInt(key: String): Option[Int] = getAs(key, _.toInt)

    def getAs[T](key: String, convert: String => T): Option[T] = params.get(key).flatMap(value => allCatch.opt(convert(value)))
  }

}

object RouteParamsSupport extends RouteParamsSupport
