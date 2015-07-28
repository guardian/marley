package com.gu.marley

import java.io.File

import com.twitter.scrooge.ThriftStruct
import org.apache.avro.file.{CodecFactory, DataFileReader, DataFileWriter}
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}

case class AvroFile[T <: ThriftStruct : AvroSerialisable](file: File) {
  val serialisable = implicitly[AvroSerialisable[T]]

  def record(t: T) = serialisable.writableValue(t).asInstanceOf[Record]

  val schema = serialisable.schema.apply()

  val writer = new GenericDatumWriter[GenericRecord](schema)
  val dataFileWriter = new DataFileWriter[GenericRecord](writer)

  dataFileWriter.setCodec(CodecFactory.snappyCodec()).create(schema, file)

  def append(structs: Iterable[T]): Unit =
    structs.map(record).foreach(dataFileWriter.append)

  def close(): Unit = dataFileWriter.close()
}

object AvroFile {

  def write[T <: ThriftStruct : AvroSerialisable](structs: Iterable[T])(file: File): Unit = {
    val avroFile = AvroFile(file)
    try {
      avroFile.append(structs)
    } finally {
      avroFile.close()
    }
  }

  def read[T <: ThriftStruct : AvroSerialisable](file: File): Iterable[T] = {
    val datumReader = new GenericDatumReader[GenericRecord]()
    val dataFileReader = new DataFileReader[GenericRecord](file, datumReader)

    import collection.convert.wrapAsScala._

    try {
      iterableAsScalaIterable(dataFileReader).map(AvroSerialisable.read[T])
    } finally {
      dataFileReader.close()
    }
  }
}
