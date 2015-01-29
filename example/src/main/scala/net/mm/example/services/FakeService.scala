package net.mm.example.services

import com.twitter.util.Future

trait FakeService {

  implicit class AsFuture[T](o: T) {
    def asFuture: Future[T] = Future(o)
  }

}
