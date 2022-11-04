// Scrooge relies on an ancient version of thrift that's not on maven central
// Instead, force a slightly more recent version
libraryDependencies += "org.apache.thrift" % "libthrift" % "0.12.0"

addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "19.10.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.12")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.14")



addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

