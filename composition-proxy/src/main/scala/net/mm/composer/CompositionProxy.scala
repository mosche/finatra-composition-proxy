package net.mm.composer

import com.twitter.finatra.FinatraServer
import net.mm.composer.properties.PropertiesParserImpl
import net.mm.composer.relations.execution.{ExecutionPlanBuilderImpl, ExecutionSchedulerImpl}
import net.mm.composer.relations.{RelationJsonComposer, RelationJsonComposerImpl, RelationRegistry}

trait CompositionProxy {
  self: FinatraServer =>

  implicit def relationComposerFactory(registry: RelationRegistry): RelationJsonComposer = {
    implicit val r = registry
    implicit val executionPlanBuilder = new ExecutionPlanBuilderImpl
    new RelationJsonComposerImpl
  }

  implicit lazy val executionScheduler = new ExecutionSchedulerImpl
  implicit lazy val propertiesParser = new PropertiesParserImpl
}
