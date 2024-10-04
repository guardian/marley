import sbtversionpolicy.withsbtrelease.ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease

ThisBuild / scalaVersion := "2.13.15"

val commonSettings = Seq(
  organization := "com.gu",
  licenses := Seq("Apache V2" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.twitter" %% "scrooge-core" % "24.2.0",
    "org.apache.thrift" % "libthrift" % "0.21.0"
  )
)

lazy val core = project.settings(
  name := "marley",
  Compile / scalacOptions ++= Seq("-release:11", "-Ymacro-annotations"),
  libraryDependencies ++= Seq(
    "org.apache.avro" % "avro" % "1.12.0",
    "org.xerial.snappy" % "snappy-java" % "1.1.10.5",
    "org.parboiled" %% "parboiled" % "2.5.1",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test,
    "org.apache.commons" % "commons-compress" % "1.27.1",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.15.4"
  ),
  Test / testOptions +=
    Tests.Argument(TestFrameworks.ScalaTest, "-u", s"test-results/scala-${scalaVersion.value}", "-o")
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
