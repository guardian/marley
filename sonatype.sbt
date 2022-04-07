publishMavenStyle := true

sonatypeProfileName := "com.gu"

(ThisBuild / publishTo) := sonatypePublishToBundle.value

(ThisBuild / scmInfo) := Some(ScmInfo(
  url("https://github.com/guardian/marley"),
  "scm:git:git@github.com:guardian/marley.git"
))

(ThisBuild / pomExtra) := {
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
