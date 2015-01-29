package net.mm.composer

import com.twitter.finatra.FinatraServer
import net.mm.composer.properties.PropertiesParserImpl
import net.mm.composer.relations.execution.{ExecutionPlanBuilderImpl, ExecutionSchedulerImpl}
import net.mm.composer.relations.{RelationJsonComposerImpl, RelationRegistry}

trait ComposingServer {
  self: FinatraServer =>

  implicit def relationRegistry: RelationRegistry

  implicit lazy val executionPlanBuilder = new ExecutionPlanBuilderImpl
  implicit lazy val executionScheduler = new ExecutionSchedulerImpl
  implicit lazy val propertiesParser = new PropertiesParserImpl
  implicit lazy val relationComposer = new RelationJsonComposerImpl
}
