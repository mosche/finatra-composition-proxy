package net.mm.example

import com.twitter.finatra._
import net.mm.composer.ComposingServer

object FinatraApp extends FinatraServer
  with ComposingServer
  with ServicesRegistry {

  System.setProperty("com.twitter.finatra.config.logNode", "")
  System.setProperty("com.twitter.finatra.config.logLevel", "DEBUG")

  register(new ExampleController)
}
