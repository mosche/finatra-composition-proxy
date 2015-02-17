package net.mm.composer

import com.twitter.util.{Await, Future}

trait FutureSupport {

  implicit class AsFuture[T](o: T) {
    def asFuture: Future[T] = Future(o)
  }

  implicit class AwaitFuture[T](f: Future[T]) {
    def await: T = Await.result(f)
  }

}

object FutureSupport extends FutureSupport
