package net.mm.composer.properties

import java.util.concurrent.TimeUnit

import net.mm.composer.relations._
import org.openjdk.jmh.annotations._

@State(Scope.Benchmark)
class PropertiesValidationBenchmark extends Extractors with MockedSources with MockCompositionControllerBuilder{

  val validator = new PropertiesValidatorImpl(classOf[Review])
  val validProperties = Right(Seq(
    FieldProperty("id"),
    FieldProperty("productId"),
    RelationProperty("product",
      FieldProperty("id"),
      RelationProperty("categories",
        FieldProperty("id")
      )
    ),
    FieldProperty("reviewerId"),
    RelationProperty("reviewer",
      FieldProperty("username"),
      RelationProperty("myreviews",
        FieldProperty("id"),
        FieldProperty("productId"),
        FieldProperty("reviewerId")
      )
    )
  ))

  val invalidProperties = Right(Seq(
    FieldProperty("id"),
    FieldProperty("productId"),
    RelationProperty("product",
      FieldProperty("id"),
      RelationProperty("categories",
        FieldProperty("id")
      )
    ),
    FieldProperty("reviewerId"),
    RelationProperty("reviewer",
      FieldProperty("username"),
      RelationProperty("invalid")
    )
  ))

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def validate_valid1: Unit = {
    assert(validator.apply(validProperties).isRight)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def validate_invalid1: Unit = {
    assert(validator.apply(invalidProperties).isLeft)
  }
}