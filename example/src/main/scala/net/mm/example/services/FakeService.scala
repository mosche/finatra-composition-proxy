package net.mm.example.services

import com.twitter.util.Future

trait FakeService {

  implicit class AsFuture[T](o: T) {
    def asFuture: Future[T] = Future(o)
  }

  implicit class RichMap[K,V](m: Map[K,V]) {
    def groupValuesBy[K](f: V => K): Map[K, Seq[V]] = m.values.toSeq.groupBy(f)
  }

}
