package net.mm.composer

import com.twitter.finatra.Controller
import net.mm.composer.relations.RelationJsonComposer

class CompositionController()(
  implicit protected val relationComposer: RelationJsonComposer
) extends Controller with CompositionResponseBuilder