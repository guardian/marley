package com.gu.mcduck

import com.twitter.scrooge.{ThriftEnum, ThriftStruct}
import org.apache.avro.Schema
import org.apache.avro.Schema.Type

import collection.convert.decorateAll._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

trait AvroSerialisable[T] {
  val schema: AvroSchema
  def writableValue(t: T): Any
  def read(x: Any): T
}

object AvroSerialisable {
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
    override def writableValue(t: Option[T]): Any = t.map(w.writableValue(_)).getOrElse(null)
    override def read(x: Any): Option[T] = Option(x).map(w.read)
  }

  def SeqAvroSerialisable[T](w: AvroSerialisable[T]) = new AvroSerialisable[Seq[T]] {
    override val schema = AvroSeqSchema(w.schema)
    override def writableValue(t: Seq[T]): Any = t.map(w.writableValue).asJava
    override def read(x: Any): Seq[T] = x.asInstanceOf[java.util.List[Any]].asScala.map(w.read).toSeq
  }

  def SetAvroSerialisable[T](w: AvroSerialisable[T]) = new AvroSerialisable[scala.collection.Set[T]] {
    override val schema = AvroSetSchema(w.schema)
    override def writableValue(t: scala.collection.Set[T]): Any = t.map(w.writableValue(_)).asJava
    override def read(x: Any): collection.Set[T] = x.asInstanceOf[java.util.List[Any]].asScala.map(w.read).toSet
  }

  def struct[T <: ThriftStruct]: AvroSerialisable[T] = macro AvroSerialisable.structMacro[T]

  def enum[T <: ThriftEnum]: AvroSerialisable[T] = macro AvroSerialisable.enumMacro[T]

  def enumMacro[T: c.WeakTypeTag](c: Context): c.Expr[AvroSerialisable[T]] = {
    import c.universe._

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

    val tree = q"""
      new com.gu.mcduck.AvroSerialisable[$typ] {
        val schema = {
          com.gu.mcduck.AvroEnumSchema(
            ${typ.typeSymbol.name.decodedName.toString},
            $pkg,
            $listMethod.map(_.name).map(com.gu.mcduck.enumsymbols.SnakesOnACamel.toSnake)
          )
        }
        val schemaInstance = schema.apply()
        val valueMap = Map($listMethod.map(x => x ->
          org.apache.avro.generic.GenericData.get.createEnum(
            com.gu.mcduck.enumsymbols.SnakesOnACamel.toSnake(x.name), schemaInstance)
        ): _*)

        def writableValue(t: $typ) = valueMap(t)

        val readMap = Map($listMethod.map(x =>
          com.gu.mcduck.enumsymbols.SnakesOnACamel.toSnake(x.name) -> x
        ): _*)

        def read(x: Any) = readMap(x.toString)
      }
    """

    c.Expr[AvroSerialisable[T]](tree)
  }

  def structMacro[T: c.WeakTypeTag](c: Context): c.Expr[AvroSerialisable[T]] = {
    import c.universe._

    val typ = weakTypeOf[T]

    val apply = typ.companion.decls.find(_.name == TermName("apply")).getOrElse(
      c.abort(c.enclosingPosition, s"Expected the companion object of $typ to have an 'apply' method")
    )

    val fields = for {
      params <- apply.asMethod.paramLists.head
      sig = params.typeSignature
    } yield {
        def neededImplicit(typ: c.universe.Type) = {
          val neededImplicitType = appliedType(weakTypeOf[AvroSerialisable[_]].typeConstructor, typ)
          val foundImplicit = c.inferImplicitValue(neededImplicitType)
          if (foundImplicit.isEmpty) c.abort(c.enclosingPosition, s"No implicit found for $neededImplicitType")
          foundImplicit
        }

        def subTypeOf(target: c.universe.Type, reference: c.universe.Type) =
          target.typeConstructor <:< reference.typeConstructor

        val optionType = typeOf[Option[_]]
        val seqType = typeOf[Seq[_]]
        val setType = typeOf[collection.Set[_]]

        def implicitFor(typ: c.universe.Type): c.Tree = typ match {
          case TypeRef(_, _, arg :: Nil) if subTypeOf(typ, optionType) =>
            q"com.gu.mcduck.AvroSerialisable.OptionAvroSerialisable(${implicitFor(arg)})"
          case TypeRef(_, _, arg :: Nil) if subTypeOf(typ, seqType) =>
            q"com.gu.mcduck.AvroSerialisable.SeqAvroSerialisable(${implicitFor(arg)})"
          case TypeRef(_, _, arg :: Nil) if subTypeOf(typ, setType) =>
            q"com.gu.mcduck.AvroSerialisable.SetAvroSerialisable(${implicitFor(arg)})"
          case _ => neededImplicit(typ)
        }

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
    val tree =
      q"""
        new com.gu.mcduck.AvroSerialisable[$typ] {
          val schema = {
            com.gu.mcduck.AvroRecordSchema(
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

    c.Expr[AvroSerialisable[T]](tree)
  }
}
