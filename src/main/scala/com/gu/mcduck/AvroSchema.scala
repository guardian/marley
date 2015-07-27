package com.gu.mcduck

import com.twitter.scrooge.ThriftEnum
import org.apache.avro.Schema.Type
import org.apache.avro.{SchemaBuilder, Schema}

import collection.convert.decorateAsJava._

trait AvroSchema {
  def apply(): Schema
}

case class SimpleAvroSchema(typ: Type) extends AvroSchema {
  def apply() = Schema.create(typ)
}

case class AvroOptionSchema(innerSchema: AvroSchema) extends AvroSchema {
  override def apply() = Schema.createUnion(List(Schema.create(Schema.Type.NULL), innerSchema()).asJava)
}

case class AvroSeqSchema(innerSchema: AvroSchema) extends AvroSchema {
  override def apply() = Schema.createArray(innerSchema())
}

case class AvroSetSchema(innerSchema: AvroSchema) extends AvroSchema {
  override def apply() = {
    val schema = Schema.createArray(innerSchema())
    schema.addProp("thrift", "set")
    schema
  }
}

case class AvroRecordSchema(name: String, namespace: String, fields: (String, AvroSchema)*) extends AvroSchema {
  def apply() = fields.foldLeft(SchemaBuilder.record(name).namespace(namespace).fields) {
    case (b, (k, v)) => b.name(k).`type`(v()).noDefault
  }.endRecord
}

case class AvroEnumSchema(name: String, namespace: String, possibleValues: List[String]) extends AvroSchema {
  override def apply() = SchemaBuilder.enumeration(name).namespace(namespace).symbols(possibleValues: _*)
}
