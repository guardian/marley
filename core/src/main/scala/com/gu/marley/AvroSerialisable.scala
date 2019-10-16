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

  implicit val booleanSerialisable = SimpleSerialisable[Boolean](Schema.Type.BOOLEAN)
  implicit val intSerialisable = SimpleSerialisable[Int](Schema.Type.INT)
  implicit val longSerialisable = SimpleSerialisable[Long](Schema.Type.LONG)
  implicit val doubleSerialisable = SimpleSerialisable[Double](Schema.Type.DOUBLE)

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

class AvroSerialisableMacro(val c: blackbox.Context) {
  import c.universe._

  def enumMacro[T: c.WeakTypeTag]: Tree = {
    val typ = weakTypeOf[T]

    def findCompanionOfThisOrParentWithMethod(method: String): Option[c.universe.Symbol] =
      (for {
        c <- typ.baseClasses
        t = c.asType.toType
        list <- t.companion.decls.find(_.name == TermName(method))
      } yield list).headOption

    val listMethod = findCompanionOfThisOrParentWithMethod("list").getOrElse(
      c.abort(c.enclosingPosition, "Expected ThriftEnum companion to have method 'list'"))

    val pkg = typ.typeSymbol.owner.fullName

    q"""
      new com.gu.marley.AvroSerialisable[$typ] {
        val schema = {
          com.gu.marley.AvroEnumSchema(
            ${typ.typeSymbol.name.decodedName.toString},
            $pkg,
            $listMethod.map(_.name).map(com.gu.marley.enumsymbols.SnakesOnACamel.toSnake)
          )
        }
        val schemaInstance = schema.apply()
        val valueMap = Map($listMethod.map(x => x ->
          org.apache.avro.generic.GenericData.get.createEnum(
            com.gu.marley.enumsymbols.SnakesOnACamel.toSnake(x.name), schemaInstance)
        ): _*)

        def writableValue(t: $typ) = valueMap(t)

        val readMap = Map($listMethod.map(x =>
          com.gu.marley.enumsymbols.SnakesOnACamel.toSnake(x.name) -> x
        ): _*)

        def read(x: Any) = readMap(x.toString)
      }
    """
  }

  def structMacro[T: c.WeakTypeTag]: Tree = {
    val typ = weakTypeOf[T]

    val apply = typ.dealias.companion.decls.find(_.name == TermName("apply")).getOrElse(
      c.abort(c.enclosingPosition, s"Expected the companion object of ${typ.dealias} to have an 'apply' method")
    )

    val fields = for {
      params <- apply.asMethod.paramLists.head
      sig = params.typeSignature
    } yield {

        (params.name.toTermName, sig, implicitFor(sig))
      }

    val fieldSchemas = fields.map { case (fieldName, fieldType, implicitSerialisable) =>
      q"""${fieldName.toString} -> $implicitSerialisable.schema"""
    }

    val fieldValues = fields.map { case (fieldName, _, implicitSerialisable) =>
      q"${fieldName.toString} -> $implicitSerialisable.writableValue(t.$fieldName)"
    }

    val fieldReaders = fields.map { case (fieldName, fieldType, implicitSerialisable) =>
      q"$implicitSerialisable.read(record.get(${fieldName.toString}))"
    }

    val pkg = typ.typeSymbol.owner.fullName

    // The _1, _2 nonsense for unpacking the Tuple is necessary here, otherwise it slays the compiler
    // I believe it's down to https://issues.scala-lang.org/browse/SI-6317
    q"""
      new com.gu.marley.AvroSerialisable[$typ] {
        val schema = {
          com.gu.marley.AvroRecordSchema(
            ${typ.typeSymbol.name.decodedName.toString},
            $pkg,
            ..$fieldSchemas
          )
        }
        val schemaInstance = schema.apply()
        def writableValue(t: $typ) = {
          Seq(..$fieldValues).foldLeft(new org.apache.avro.generic.GenericRecordBuilder(schemaInstance)){(b, nv) =>
            b.set(nv._1, nv._2)
          }.build
        }

        def read(x: Any) = {
          val record = x.asInstanceOf[org.apache.avro.generic.GenericRecord]
          $apply(..$fieldReaders)
        }
      }
      """
  }

  def unionMacro[T: c.WeakTypeTag]: Tree = {
    implicit val symbolOrdering: Ordering[c.universe.Symbol] = Ordering.by(_.fullName)

    val typ = weakTypeOf[T].dealias
    if (typ.typeSymbol.isAbstract) {
      val subClasses: Seq[c.universe.Symbol] = // we need reproducible ordering for schema fingerprinting!
        typ.typeSymbol.asClass.knownDirectSubclasses.toSeq.sorted

      val cases = subClasses map { cl =>
        val typ = tq"${cl.asType}"
        val pat = pq"""x : $typ"""
        val grTyp = pq"""x : org.apache.avro.generic.GenericRecord"""
        // for compatibility reasons
        if (cl.name.toString == "UnknownUnionField") {
          (
            Option.empty[c.universe.Tree],
            cq"""$pat => throw new RuntimeException("Can't serialise UnknownUnionField")"""
            )
        }
        else {
          val format = implicitFor(cl.asType.toType)
          (
            Some(q"""$format.read(x)"""),
            cq"""$pat => { $format.writableValue(x) }
            """
            )
        }
      }
      val schemas = subClasses flatMap { cl =>
        if (cl.name.toString == "UnknownUnionField") {
          None
        } else {
          val format = implicitFor(cl.asType.toType)
          Some(q"""$format.schema""")
        }
      }
      val (readCases, writeCases) = cases.unzip

      val readTree = readCases.flatten.tail.foldLeft(q"scala.util.Try(${readCases.flatten.head})")((tree, reader) =>
        q"$tree.recover{case _ => $reader}"
      )

      q"""
        new com.gu.marley.AvroSerialisable[$typ] {
          val schema = com.gu.marley.AvroUnionSchema(
            ${schemas.toList}
          )
          val schemaInstance = schema.apply()
          def read(x: Any) = $readTree.get
          def writableValue(a: $typ): Any = {
            a match { case ..$writeCases }
          }
        }
      """
    }
    else {
      structMacro[T]
    }
  }

  private def neededImplicit(typ: c.universe.Type) = {
    val neededImplicitType = appliedType(weakTypeOf[AvroSerialisable[_]].typeConstructor, typ.dealias)
    val foundImplicit = c.inferImplicitValue(neededImplicitType)
    if (foundImplicit.nonEmpty) {
      foundImplicit
    } else {
      c.abort(c.enclosingPosition, s"Expected the companion object of ${typ.dealias} to have an 'apply' method")
    }
  }

  private def subTypeOf(target: c.universe.Type, reference: c.universe.Type) =
    target.typeConstructor <:< reference.typeConstructor

  private val optionType = typeOf[Option[_]]
  private val seqType = typeOf[collection.Seq[_]]
  private val setType = typeOf[collection.Set[_]]
  private val mapType = typeOf[collection.Map[_, _]]

  private val StringType = typeOf[String]

  private def implicitFor(typ: c.universe.Type): c.Tree = typ match {
    case TypeRef(_, _, arg :: Nil) if subTypeOf(typ, optionType) =>
      q"com.gu.marley.AvroSerialisable.OptionAvroSerialisable(${implicitFor(arg)})"
    case TypeRef(_, _, arg :: Nil) if subTypeOf(typ, seqType) =>
      q"com.gu.marley.AvroSerialisable.SeqAvroSerialisable(${implicitFor(arg)})"
    case TypeRef(_, _, arg :: Nil) if subTypeOf(typ, setType) =>
      q"com.gu.marley.AvroSerialisable.SetAvroSerialisable(${implicitFor(arg)})"
    case TypeRef(_, _, StringType :: arg :: Nil) if subTypeOf(typ, mapType) =>
      q"com.gu.marley.AvroSerialisable.MapAvroSerialisable(${implicitFor(arg)})"
    case _ => neededImplicit(typ)
  }

}
