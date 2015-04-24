import sbt.Defaults._
import sbt.Keys._
import sbt._
import pl.project13.scala.sbt.SbtJmh.jmhSettings

object Build extends Build{

  lazy val standardSettings = coreDefaultSettings ++ Seq(
    scalaVersion := "2.10.5",
    resolvers += "Twitter" at "http://maven.twttr.com",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finatra" % "1.6.0",
      "org.scalatest" % "scalatest_2.10" % "2.2.3" % "test",
      "org.mockito" % "mockito-core" % "1.10.19" % "test"
    )
  )

  lazy val `composition-proxy` = project
    .settings(standardSettings:_*)

  lazy val `benchmark` = project
    .settings(standardSettings:_*)
    .settings(jmhSettings:_*)
    .dependsOn(`composition-proxy` % "compile->compile;compile->test")

  lazy val example = project
    .settings(standardSettings:_*)
    .settings(mainClass := Some("net.mm.example.FinatraApp"))
    .dependsOn(`composition-proxy`)
}
