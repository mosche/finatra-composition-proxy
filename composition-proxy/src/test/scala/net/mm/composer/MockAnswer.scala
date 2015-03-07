package net.mm.composer

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar.mock

case class MockAnswer[T](answer: T) extends Answer[T]{
  override def answer(invocation: InvocationOnMock): T = answer
}

object MockAnswer{
  def apply[T <: AnyRef]()(implicit m: Manifest[T]) = new MockAnswer(mock[T])
}
