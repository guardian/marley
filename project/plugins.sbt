// Scrooge relies on libthrift at build time for thrift generation.
libraryDependencies += "org.apache.thrift" % "libthrift" % "0.23.0"

addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "22.12.0")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "3.2.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")
