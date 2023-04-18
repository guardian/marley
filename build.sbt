val commonSettings = Seq(
  organization := "com.gu",
  homepage := Some(url("https://github.com/guardian/marley")),
  licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  libraryDependencies ++= Seq(
    ("com.twitter" %% "scrooge-core" % "22.12.0").cross(CrossVersion.for3Use2_13),
    "org.apache.thrift" % "libthrift" % "0.17.0"
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      )
      case _ => Seq.empty
  })
)

lazy val core = project.settings(
  name := "marley",
  Compile / scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Some("-Ymacro-annotations")
    // case Some((3, _)) => Some("-explain")
    case _ => None
  }),
  scalaVersion := "3.2.2",
  crossScalaVersions := Seq(scalaVersion.value, "2.13.10"),
  libraryDependencies ++= Seq(
    "org.apache.avro" % "avro" % "1.7.7",
    "org.parboiled" %% "parboiled" % "2.4.1",
    "org.scalatest" %% "scalatest" % "3.2.15" % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % "3.2.15.0" % Test
  ),
  Test/testOptions += Tests.Argument(
    TestFrameworks.ScalaTest,
    "-u", s"test-results/scala-${scalaVersion.value}"
  )
).settings(commonSettings: _*).dependsOn(thriftExample % "test->test")

lazy val thriftExample = project.settings(commonSettings: _*).settings(
  scalaVersion := "2.13.10"
)

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

