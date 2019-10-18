publishMavenStyle := true

sonatypeProfileName := "com.gu"

publishTo in ThisBuild := sonatypePublishToBundle.value

scmInfo in ThisBuild := Some(ScmInfo(
  url("https://github.com/guardian/marley"),
  "scm:git:git@github.com:guardian/marley.git"
))

pomExtra in ThisBuild := {
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
