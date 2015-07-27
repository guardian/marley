package com.gu.mcduck

import java.io.File

import org.scalatest.{Matchers, FlatSpec}

class AvroFileTest extends FlatSpec with Matchers {
  it should "read back a file of page views it writes" in {
    val struct = ExampleStruct("reqString", None, ExampleEnum.A1, true, 1.0d)

    val file = File.createTempFile("AvroTest-read-write", ".avro")
    file.deleteOnExit()

    implicit val enumSer = AvroSerialisable.enum[ExampleEnum]
    implicit val subStructSer = AvroSerialisable.struct[SubStruct]
    implicit val structSer = AvroSerialisable.struct[ExampleStruct]

    AvroFile.write(Seq(struct))(file)

    val readPageViews = AvroFile.read[ExampleStruct](file)
    readPageViews.toList should be (List(struct))
  }
}
