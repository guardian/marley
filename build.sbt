import sbtversionpolicy.withsbtrelease.ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease

ThisBuild / scalaVersion := "2.13.12"

val commonSettings = Seq(
  organization := "com.gu",
  licenses := Seq("Apache V2" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.twitter" %% "scrooge-core" % "22.12.0",
    "org.apache.thrift" % "libthrift" % "0.17.0"
  )
)

lazy val core = project.settings(
  name := "marley",
  Compile / scalacOptions ++= Seq("-release:11", "-Ymacro-annotations"),
  libraryDependencies ++= Seq(
    "org.apache.avro" % "avro" % "1.11.3" exclude("org.apache.commons", "commons-compress"),
    "org.xerial.snappy" % "snappy-java" % "1.1.10.5",
    "org.parboiled" %% "parboiled" % "2.5.0",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    "org.scalatest" %% "scalatest" % "3.2.16" % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.16.0" % Test,
    "org.apache.commons" % "commons-compress" % "1.26.2"
  ),
  Test/testOptions += Tests.Argument(
    TestFrameworks.ScalaTest,
    "-u", s"test-results/scala-${scalaVersion.value}"
  )
).settings(commonSettings).dependsOn(thriftExample % "test->test")

lazy val thriftExample = project.settings(commonSettings).settings(
  publish / skip := true,
)

lazy val root = (project in file(".")).aggregate(core).settings(
  update / aggregate := false,
  publish / skip := true,
)

import ReleaseTransformations.*

releaseVersion := fromAggregatedAssessedCompatibilityWithLatestRelease().value
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)