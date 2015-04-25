package net.mm.composer.relations.execution

import java.util.concurrent.TimeUnit

import com.twitter.util.Await
import com.twitter.util.FuturePool.unboundedPool
import net.mm.composer.relations.Relation.Source
import net.mm.composer.relations._
import org.openjdk.jmh.annotations._

@State(Scope.Benchmark)
class ExecutionSchedulerBenchmark extends Extractors with Sources {

  val scheduler = new ExecutionSchedulerImpl

  val products = Seq(
    Product(1, "computer", "laptop"), Product(2, "computer", "hardware", "memory"),
    Product(3, "hardware", "storage"), Product(4, "storage", "external", "usb"),
    Product(5, "audio", "usb"), Product(6, "computer", "hardware", "usb")
  )

  val categoriesByProduct = products.map(p => (p.id, p.categoryIds.map(Category))).toMap

  val users = Seq("steff","mark","chris","daniel","michaela","dani","claudi").map(id => (id,User(id))).toMap

  val reviews = Seq(
    Review(1, 1, "steff"), Review(2,1,"daniel"), Review(3,1,"dani"), Review(4,1,"claudi"),Review(5, 6, "chris"),
    Review(6, 2, "mark"), Review(7,2,"michaela"), Review(8,2,"steff"), Review(9,2,"dani"),Review(10, 6, "michaela"),
    Review(11, 3, "chris"), Review(12,3,"michaela"), Review(13,3,"daniel"), Review(14,3,"dani"),Review(15, 1, "michaela"),
    Review(11, 4, "daniel"), Review(12,4,"dani"), Review(13,4,"chris"), Review(14,4,"claudi"),Review(15, 6, "daniel"),
    Review(11, 5, "daniel"), Review(12,5,"michaela"), Review(13,5,"steff"), Review(14,5,"claudi"),Review(15, 1, "daniel")
  )

  val reviewsByProduct = reviews.groupBy(_.productId)

  override val getCategoriesByProduct: Source[Int, Seq[Category]] = ids => unboundedPool{
    categoriesByProduct.filterKeys(ids)
  }

  override val getReviewsByProduct: Source[Int, Seq[Review]] = ids => unboundedPool{
    reviewsByProduct.filterKeys(ids)
  }

  override val getUsers: Source[String, User] = ids => unboundedPool{
    users.filterKeys(ids)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def independentExecution: Unit = {
    val plan = Seq(
      TaskNode("categories", ToMany(productIdExtractor, getCategoriesByProduct)),
      TaskNode("reviews", ToMany(productIdExtractor, getReviewsByProduct),
        TaskNode("reviewer", ToOne(userIdExtractor, getUsers))
      )
    )

    val result = Await.result(scheduler.run(plan)(products))

    assert(result.dataMap.contains(getReviewsByProduct))
    assert(result.dataMap.contains(getCategoriesByProduct))
    assert(result.dataMap.contains(getUsers))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def joinedExecution: Unit = {
    val plan = Seq(
      TaskNode("categories", ToMany(productIdExtractor, getCategoriesByProduct)),
      TaskNode("reviews", ToMany(productIdExtractor, getReviewsByProduct),
        TaskNode("categories", ToMany(productIdExtractor, getCategoriesByProduct)),
        TaskNode("reviewer", ToOne(userIdExtractor, getUsers))
      )
    )

    val result = Await.result(scheduler.run(plan)(products))

    assert(result.dataMap.contains(getReviewsByProduct))
    assert(result.dataMap.contains(getCategoriesByProduct))
    assert(result.dataMap.contains(getUsers))
  }

}
