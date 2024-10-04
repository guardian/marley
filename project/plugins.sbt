// Scrooge relies on an ancient version of thrift that's not on maven central
// Instead, force a slightly more recent version
libraryDependencies += "org.apache.thrift" % "libthrift" % "0.21.0"

addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "24.2.0")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.4.0")
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "3.2.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")
