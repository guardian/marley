lazy val root = (project in file(".")).aggregate(core).settings(aggregate in update := false)

lazy val core = project.settings(
  name := "marley",

  libraryDependencies ++= Seq(
    "org.apache.avro" % "avro" % "1.7.7",
    "org.parboiled" %% "parboiled" % "2.1.0",
    "org.typelevel" %% "macro-compat" % "1.1.1",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    "org.scalatest" %% "scalatest" % "2.2.5" % Test,
    "org.scalacheck" %% "scalacheck" % "1.12.4" % Test
  )
).settings(commonSettings: _*).settings(publishSettings: _*).dependsOn(thriftExample % "test->test")

lazy val thriftExample = project.settings(commonSettings: _*)

val commonSettings = Seq(
  organization := "com.gu",

  scalaVersion := "2.11.12",
  crossScalaVersions := Seq(scalaVersion.value),

  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.twitter" %% "scrooge-core" % "4.6.0",
    "org.apache.thrift" % "libthrift" % "0.9.2"
  )
)

val publishSettings = Seq(
  homepage := Some(url("https://github.com/guardian/marley")),
  licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  scmInfo := Some(ScmInfo(
    url("https://github.com/guardian/marley"),
    "scm:git:git@github.com:guardian/marley.git"
  )),
  pomExtra := {
    <developers>
      <developer>
        <id>philwills</id>
        <name>Phil Wills</name>
        <url>https://github.com/philwills</url>
      </developer>
      <developer>
        <id>emma-p</id>
        <name>Emmanuelle Poirier</name>
        <url>https://github.com/emma-p</url>
      </developer>
    </developers>
  }
)

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)
