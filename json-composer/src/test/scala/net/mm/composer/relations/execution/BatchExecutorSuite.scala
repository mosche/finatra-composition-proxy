package net.mm.composer.relations.execution

import com.twitter.util.Future
import net.mm.composer.FutureSupport._
import org.mockito.Mockito.{spy, verify, verifyNoMoreInteractions, verifyZeroInteractions}
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class BatchExecutorSuite extends FunSuite {

  class Fixture extends Function1[Set[Int], Future[Map[Int, Int]]] {
    def apply(ids: Set[Int]): Future[Map[Int, Int]] = ids.map(i => (i, i)).toMap.asFuture
  }

  val executor = spy(new Fixture)
  val batchExecutor = new BatchExecutor(executor)

  test("ids must be registered before execution") {
    intercept[AssertionError](
      batchExecutor.execute(Set(1))
    )
    verifyZeroInteractions(executor)
  }

  test("execute added ids only once") {
    batchExecutor.addIds(Set(1, 2))
    batchExecutor.addIds(Set(3))

    batchExecutor.execute(Set(1, 3)).await shouldBe Map(1 -> 1, 3 -> 3)
    verify(executor).apply(Set(1, 2, 3))

    batchExecutor.execute((Set(2))).await shouldBe Map(2 -> 2)
    verifyNoMoreInteractions(executor)
  }

  test("getResult of previous execution") {
    batchExecutor.getResult() shouldBe Map(1 -> 1, 2 -> 2, 3 -> 3)
    verifyNoMoreInteractions(executor)
  }

}
