package net.mm.composer.utils

import java.util.UUID
import scala.util.control.Exception._

class ParamConverter[A](f: String => Option[A]) extends (String => Option[A]){
  override def apply(s: String): Option[A] = f(s)
}

object ParamConverter {
  implicit val stringConverter = new ParamConverter[String](Some(_))

  implicit val intConverter = new ParamConverter[Int](s => allCatch.opt(s.toInt))

  implicit val uuidConverter = new ParamConverter[UUID](s => allCatch.opt(UUID.fromString(s)))
}
