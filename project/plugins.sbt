// Scrooge relies on an ancient version of thrift that's not on maven central
// Instead, force a slightly more recent version
libraryDependencies += "org.apache.thrift" % "libthrift" % "0.6.1"

addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "4.5.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.5.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
