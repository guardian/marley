name := "mcduck"

scalaVersion := "2.11.7"

com.twitter.scrooge.ScroogeSBT.newSettings

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
	"com.twitter" %% "scrooge-core" % "3.17.0",
	"org.apache.thrift" % "libthrift" % "0.9.2",
  "org.apache.avro" % "avro" % "1.7.7",
  "org.parboiled" %% "parboiled" % "2.1.0",
  "org.scalatest" %% "scalatest" % "2.2.5" % Test,
  "org.scalacheck" %% "scalacheck" % "1.12.4" % Test
)
