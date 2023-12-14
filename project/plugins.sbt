// Scrooge relies on an ancient version of thrift that's not on maven central
// Instead, force a slightly more recent version
libraryDependencies += "org.apache.thrift" % "libthrift" % "0.17.0"

addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "22.12.0")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "3.2.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")
