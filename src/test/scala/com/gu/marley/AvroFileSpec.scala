package com.gu.marley

import java.io.File

import org.scalatest.prop.Checkers
import org.scalatest.FlatSpec
import org.scalacheck._
import Gen._
import Arbitrary.arbitrary

class AvroFileSpec extends FlatSpec with Checkers {
  it should "read back a file of page views it writes" in {

    val arbSubStruct = for {
      b <- arbitrary[Boolean]
    } yield SubStruct(b)

    val arbSubUnion1: Gen[ExampleUnion] = for {
      s <- arbitrary[String]
    } yield ExampleUnion.Subunion1(SubUnion1(s))

    val arbSubUnion2: Gen[ExampleUnion] = for {
      s <- arbitrary[String]
    } yield ExampleUnion.Subunion2(SubUnion2(s))

    val arbUnion = oneOf[ExampleUnion](arbSubUnion1, arbSubUnion2)
    val arbEnum = oneOf(ExampleEnum.A1, ExampleEnum.B2, ExampleEnum.C3)

    implicit val arbStruct: Arbitrary[ExampleStruct] = Arbitrary(for {
      string <- arbitrary[String]
      maybeString <- arbitrary[Option[String]]
      enum <- arbEnum
      bool <- arbitrary[Boolean]
      double <- arbitrary[Double]
      seq <- arbitrary[Option[Seq[String]]]
      substruct <- arbSubStruct
      set <- arbitrary[Option[Set[String]]]
      int <- arbitrary[Option[Int]]
      short <- arbitrary[Option[Short]]
      long <- arbitrary[Option[Long]]
      byte <- arbitrary[Option[Byte]]
      map <- arbitrary[Option[Map[String, Int]]]
      union <- arbUnion
    } yield ExampleStruct(
        string, maybeString, enum, defaultBool = bool, double,
        seq, substruct, set,
        int, short, long, byte, map, union
      ))

    implicit val enumSer = AvroSerialisable.enum[ExampleEnum]
    implicit val subStructSer = AvroSerialisable.struct[SubStruct]
    implicit val structSer = AvroSerialisable.struct[ExampleStruct]
    implicit val unionSer = AvroSerialisable.union[ExampleUnion]

    check { (struct: ExampleStruct) =>

      val file = File.createTempFile("AvroTest-read-write", ".avro")
      file.deleteOnExit()

      AvroFile.write(Seq(struct))(file)

      AvroFile.read[ExampleStruct](file).head == struct
    }
  }
}
