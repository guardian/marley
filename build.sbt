scalaVersion in ThisBuild := "2.12.10"

crossScalaVersions in ThisBuild := Seq(scalaVersion.value, "2.11.12")

val commonSettings = Seq(
  organization := "com.gu",
  homepage := Some(url("https://github.com/guardian/marley")),
  licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.twitter" %% "scrooge-core" % "19.10.0",
    "org.apache.thrift" % "libthrift" % "0.12.0"
  )
)

lazy val core = project.settings(
  name := "marley",
  libraryDependencies ++= Seq(
    "org.apache.avro" % "avro" % "1.7.7",
    "org.parboiled" %% "parboiled" % "2.1.8",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
    "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    "org.scalacheck" %% "scalacheck" % "1.14.2" % Test
  )
).settings(commonSettings: _*).dependsOn(thriftExample % "test->test")

lazy val thriftExample = project.settings(commonSettings: _*)

publishArtifact in Test := false

lazy val root = (project in file(".")).aggregate(core).settings(
  aggregate in update := false,
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

import ReleaseTransformations._
import sbt.Keys.scalaBinaryVersion

releaseCrossBuild := true // true if you cross-build the project for multiple Scala versions

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // For non cross-build projects, use releaseStepCommand("publishSigned")
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

