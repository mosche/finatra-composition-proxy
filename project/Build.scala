import sbt.Defaults._
import sbt.Keys._
import sbt._

object Build extends Build{

  lazy val standardSettings = coreDefaultSettings ++ Seq(
    scalaVersion := "2.10.4",
    resolvers += "Twitter" at "http://maven.twttr.com",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finatra" % "1.6.0",
      "org.scalatest" % "scalatest_2.10" % "2.2.3" % "test",
      "org.mockito" % "mockito-core" % "1.10.19" % "test"
    )
  )

  lazy val `json-composer` = project
    .settings(standardSettings:_*)

  lazy val example = project
    .settings(standardSettings:_*)
    .settings(mainClass := Some("net.mm.example.FinatraApp"))
    .dependsOn(`json-composer`)
}
