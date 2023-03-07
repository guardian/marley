package com.gu.marley

import com.twitter.scrooge.ThriftStruct

import java.io.File
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.*
import org.scalacheck.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.Checkers

class AvroFileSpec extends AnyFlatSpec with Checkers with Matchers {
  val union = ExampleUnion.Subunion1(SubUnion1("id"))

  def checkRoundTrip[T](v: T)(as: AvroSerialisable[T]): Unit = {
    as.read(as.writableValue(v)) shouldEqual v
  }

  it should "handle an enum" in {
    implicit val enumSer: AvroSerialisable[ExampleEnum] = AvroSerialisable.`enum`[ExampleEnum]

    checkRoundTrip(ExampleEnum.A1)
    checkRoundTrip(ExampleEnum.B2)
    checkRoundTrip(ExampleEnum.C3)
  }

  it should "handle a union" in {
    implicit val unionSer: AvroSerialisable[ExampleUnion] = AvroSerialisable.union[ExampleUnion]
    checkRoundTrip(union)
  }

  it should "handle a struct" in {
    implicit val subStructSer: AvroSerialisable[SubStruct] = AvroSerialisable.struct[SubStruct]

    checkRoundTrip(SubStruct(falseOrTrue = true))
  }

  it should "read back a file of page views it writes" in {

    val arbSubStruct = for {
      b <- arbitrary[Boolean]
    } yield SubStruct(b)

    val arbEnum = org.scalacheck.Gen.oneOf(ExampleEnum.A1, ExampleEnum.B2, ExampleEnum.C3)

    implicit val arbStruct: Arbitrary[ExampleStruct] = Arbitrary(for {
      string <- arbitrary[String]
      maybeString <- arbitrary[Option[String]]
      anEnum <- arbEnum
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
        string, maybeString, anEnum, defaultBool = bool, double,
        seq, substruct, set,
        int, short, long, byte, map, union
      ))

    implicit val enumSer: AvroSerialisable[ExampleEnum] = AvroSerialisable.`enum`[ExampleEnum]
    implicit val unionSer: AvroSerialisable[ExampleUnion] = AvroSerialisable.union[ExampleUnion]
    implicit val subStructSer: AvroSerialisable[SubStruct] = AvroSerialisable.struct[SubStruct]
    implicit val structSer: AvroSerialisable[ExampleStruct] = AvroSerialisable.struct[ExampleStruct]

    check { (struct: ExampleStruct) =>
      val file = File.createTempFile("AvroTest-read-write", ".avro")
      file.deleteOnExit()

      AvroFile.write(Seq(struct))(file)

      AvroFile.read[ExampleStruct](file).head == struct
    }
  }

}
