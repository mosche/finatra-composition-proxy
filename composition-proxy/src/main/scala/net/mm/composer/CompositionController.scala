package net.mm.composer

import com.twitter.finatra.Controller
import net.mm.composer.properties.PropertiesParser
import net.mm.composer.relations.RelationJsonComposer

class CompositionController()(implicit protected val propertiesParser: PropertiesParser, protected val relationComposer: RelationJsonComposer)
  extends Controller
  with CompositionResponseBuilder