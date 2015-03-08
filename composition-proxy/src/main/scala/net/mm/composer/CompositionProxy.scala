package net.mm.composer

import com.twitter.finatra.FinatraServer
import net.mm.composer.properties.{FieldPropertyRewriterImpl, PropertiesParser, PropertiesParserImpl, PropertiesValidatorImpl}
import net.mm.composer.relations.execution.{ExecutionPlanBuilderImpl, ExecutionSchedulerImpl}
import net.mm.composer.relations.{RelationJsonComposer, RelationJsonComposerImpl, RelationRegistry}

trait CompositionProxy {
  self: FinatraServer =>

  implicit lazy val relationComposerFactory: RelationRegistry => RelationJsonComposer = implicit registry => {
    implicit val executionPlanBuilder = new ExecutionPlanBuilderImpl
    new RelationJsonComposerImpl
  }

  implicit lazy val propertiesParserFactory: RelationRegistry => Class[_] => PropertiesParser = implicit registry => clazz => {
    new PropertiesParserImpl andThen new FieldPropertyRewriterImpl(clazz) andThen new PropertiesValidatorImpl(clazz)
  }

  implicit lazy val executionScheduler = new ExecutionSchedulerImpl
}
