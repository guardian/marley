package com.gu.marley
import com.twitter.scrooge.{ThriftEnum, ThriftStruct, ThriftUnion}
import org.apache.avro.Schema
import org.apache.avro.Schema.Type

import collection.JavaConverters._
import scala.language.experimental.macros
import scala.reflect.macros.blackbox


trait AvroSerialisable[T] {
  val schema: AvroSchema
  def writableValue(t: T): Any
  def read(x: Any): T
}

object AvroSerialisable extends LowPriorityImplicitSerialisable {
  def value[T](t: T)(implicit lw: AvroSerialisable[T]): Any = lw.writableValue(t)
  def read[T](x: Any)(implicit lw: AvroSerialisable[T]): T = lw.read(x)
  def schema[T](implicit lw: AvroSerialisable[T]): AvroSchema = lw.schema

  implicit val booleanSerialisable: SimpleSerialisable[Boolean] = SimpleSerialisable[Boolean](Schema.Type.BOOLEAN)
  implicit val intSerialisable: SimpleSerialisable[Int] = SimpleSerialisable[Int](Schema.Type.INT)
  implicit val longSerialisable: SimpleSerialisable[Long] = SimpleSerialisable[Long](Schema.Type.LONG)
  implicit val doubleSerialisable: SimpleSerialisable[Double] = SimpleSerialisable[Double](Schema.Type.DOUBLE)

  case class SimpleSerialisable[T](typ: Type) extends AvroSerialisable[T] {
    override val schema = SimpleAvroSchema(typ)
    override def writableValue(t: T) = t
    override def read(x: Any): T = x.asInstanceOf[T]
  }

  implicit object StringSerialisable extends AvroSerialisable[String] {
    override val schema = new AvroSchema {
      override def apply() = Schema.create(Schema.Type.STRING)
    }
    override def writableValue(t: String): Any = t
    override def read(x: Any): String = x.asInstanceOf[CharSequence].toString
  }

  implicit object ShortSerialisable extends AvroSerialisable[Short] {
    override val schema = new AvroSchema {
      override def apply() = {
        val s = Schema.create(Schema.Type.INT)
        s.addProp("thrift", "short")
        s
      }
    }
    override def writableValue(t: Short): Any = t.toInt
    override def read(x: Any): Short = x.asInstanceOf[Int].toShort
  }

  implicit object ByteSerialisable extends AvroSerialisable[Byte] {
    override val schema = new AvroSchema {
      override def apply() = {
        val s = Schema.create(Schema.Type.INT)
        s.addProp("thrift", "byte")
        s
      }
    }
    override def writableValue(t: Byte): Any = t.toInt
    override def read(x: Any): Byte = x.asInstanceOf[Int].toByte
  }

  def OptionAvroSerialisable[T](w: AvroSerialisable[T]) = new AvroSerialisable[Option[T]] {
    override val schema = AvroOptionSchema(w.schema)
    override def writableValue(t: Option[T]): Any = t.map(w.writableValue).orNull
    override def read(x: Any): Option[T] = Option(x).map(w.read)
  }

  def SeqAvroSerialisable[T](w: AvroSerialisable[T]) = new AvroSerialisable[collection.Seq[T]] {
    override val schema = AvroSeqSchema(w.schema)
    override def writableValue(t: collection.Seq[T]): Any = t.map(w.writableValue).asJava
    override def read(x: Any): collection.Seq[T] = x.asInstanceOf[java.util.List[Any]].asScala.map(w.read).toSeq
  }

  def SetAvroSerialisable[T](w: AvroSerialisable[T]) = new AvroSerialisable[scala.collection.Set[T]] {
    override val schema = AvroSetSchema(w.schema)
    override def writableValue(t: scala.collection.Set[T]): Any = t.map(w.writableValue).asJava
    override def read(x: Any): collection.Set[T] = x.asInstanceOf[java.util.List[Any]].asScala.map(w.read).toSet
  }

  def MapAvroSerialisable[V](wv: AvroSerialisable[V]) =
    new AvroSerialisable[collection.Map[String,V]] {
      override val schema = AvroMapSchema(wv.schema)
      override def writableValue(t: collection.Map[String, V]): Any = t.map {
        case (k,v) => (StringSerialisable.writableValue(k), wv.writableValue(v))
      }.asJava
      override def read(x: Any): collection.Map[String,V] =
        x.asInstanceOf[java.util.Map[Any,Any]].asScala.map {
          case (k, v) => (StringSerialisable.read(k), wv.read(v))
        }
    }

  implicit def enum[T <: ThriftEnum]: AvroSerialisable[T] = macro AvroSerialisableMacro.enumMacro[T]

  implicit def union[T <: ThriftUnion]: AvroSerialisable[T] = macro AvroSerialisableMacro.unionMacro[T]

}

trait LowPriorityImplicitSerialisable {
  implicit def struct[T <: ThriftStruct]: AvroSerialisable[T] = macro AvroSerialisableMacro.structMacro[T]
}

