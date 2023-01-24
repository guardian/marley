package com.gu.marley

import java.io.File

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen._
import org.scalacheck._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.Checkers

class AvroFileSpec extends AnyFlatSpec with Checkers {
  val union = ExampleUnion.Subunion1(SubUnion1("id"))

  it should "read back a file of page views it writes" in {

    val arbSubStruct = for {
      b <- arbitrary[Boolean]
    } yield SubStruct(b)

    val arbEnum = oneOf(ExampleEnum.A1, ExampleEnum.B2, ExampleEnum.C3)

    implicit val arbStruct: Arbitrary[ExampleStruct] = Arbitrary(for {
      string <- arbitrary[String]
      maybeString <- arbitrary[Option[String]]
      enum <- arbEnum
      bool <- arbitrary[Boolean]
      double <- arbitrary[Double]
      seq <- arbitrary[Option[collection.Seq[String]]]
      substruct <- arbSubStruct
      set <- arbitrary[Option[Set[String]]]
      int <- arbitrary[Option[Int]]
      short <- arbitrary[Option[Short]]
      long <- arbitrary[Option[Long]]
      byte <- arbitrary[Option[Byte]]
      map <- arbitrary[Option[Map[String, Int]]]
    } yield ExampleStruct(
        string, maybeString, enum, defaultBool = bool, double,
        seq, substruct, set,
        int, short, long, byte, map, union
      ))

    check { (struct: ExampleStruct) =>

      implicit val enumSer = AvroSerialisable.enum[ExampleEnum]
      implicit val subStructSer = AvroSerialisable.struct[SubStruct]
      implicit val unionSer = AvroSerialisable.union[ExampleUnion]
      implicit val structSer = AvroSerialisable.struct[ExampleStruct]

      val file = File.createTempFile("AvroTest-read-write", ".avro")
      file.deleteOnExit()

      AvroFile.write(Seq(struct))(file)

      AvroFile.read[ExampleStruct](file).head == struct
    }
  }
}
