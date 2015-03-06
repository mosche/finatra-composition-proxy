package net.mm.composer.utils

trait SeqTypeFilterSupport {
  implicit class TypeFilter[S](seq:Seq[S]){
    def typeFilter[T <: S](implicit m: Manifest[T]): Seq[T] = seq.filter(m.runtimeClass.isInstance).asInstanceOf[Seq[T]]
  }
}

object SeqTypeFilterSupport extends SeqTypeFilterSupport
