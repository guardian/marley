ThisBuild/scalaVersion := "2.13.12"

ThisBuild/crossScalaVersions := Seq(scalaVersion.value)

val commonSettings = Seq(
  organization := "com.gu",
  homepage := Some(url("https://github.com/guardian/marley")),
  licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.twitter" %% "scrooge-core" % "22.12.0",
    "org.apache.thrift" % "libthrift" % "0.17.0"
  )
)

def versionDependent[T](scalaV: String, handlesAnnotations: Boolean, value: T):Option[T] = {
  val preMacroAnnotationsScalaVersions = Set("2.11", "2.12")
  Some(value).filter(_ => handlesAnnotations != preMacroAnnotationsScalaVersions.contains(scalaV))
}

lazy val core = project.settings(
  name := "marley",
  Compile / scalacOptions += versionDependent(scalaBinaryVersion.value, handlesAnnotations=true, "-Ymacro-annotations"),
  libraryDependencies ++= versionDependent(scalaBinaryVersion.value, handlesAnnotations=false,
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)).toSeq ++ Seq(
    "org.apache.avro" % "avro" % "1.11.1",
    "org.xerial.snappy" % "snappy-java" % "1.1.10.0",
    "org.parboiled" %% "parboiled" % "2.5.0",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    "org.scalatest" %% "scalatest" % "3.2.16" % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.16.0" % Test
  ),
  Test/testOptions += Tests.Argument(
    TestFrameworks.ScalaTest,
    "-u", s"test-results/scala-${scalaVersion.value}"
  )
).settings(commonSettings: _*).dependsOn(thriftExample % "test->test")

lazy val thriftExample = project.settings(commonSettings: _*)

Test/publishArtifact := false

lazy val root = (project in file(".")).aggregate(core).settings(
  update/aggregate := false,
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

