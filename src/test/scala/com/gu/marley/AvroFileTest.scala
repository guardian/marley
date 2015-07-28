package com.gu.marley

import java.io.File

import org.scalatest.{Matchers, FlatSpec}

class AvroFileTest extends FlatSpec with Matchers {
  it should "read back a file of page views it writes" in {
    val struct = ExampleStruct(
      "reqString", None, ExampleEnum.A1, defaultBool = true, 1.0d,
      Some(List("1", "2", "x")), Some(SubStruct(true)), Some(Set("y", "4")),
      Some(32), Some(16.toShort), Some(64L), Some(8.toByte), Some(Map("quick" -> 1, "brown" -> 2, "fox" -> 3))
    )

    val file = File.createTempFile("AvroTest-read-write", ".avro")
    file.deleteOnExit()

    implicit val enumSer = AvroSerialisable.enum[ExampleEnum]
    implicit val subStructSer = AvroSerialisable.struct[SubStruct]
    implicit val structSer = AvroSerialisable.struct[ExampleStruct]

    AvroFile.write(Seq(struct))(file)

    val readStructs = AvroFile.read[ExampleStruct](file)

    println(readStructs.head.stringIntMap.get.keySet.map(_.getClass))
    readStructs.head.stringIntMap.get.keySet should be (struct.stringIntMap.get.keySet)
    readStructs.head.stringIntMap.get.values.toSet should be (struct.stringIntMap.get.values.toSet)

    readStructs.toList should be (List(struct))
  }
}
