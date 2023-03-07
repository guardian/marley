package com.gu.marley

import com.twitter.scrooge.{ThriftEnum, ThriftEnumObject, ThriftStruct}

import scala.quoted.*

object AvroSerialisableMacro {
    def enumMacro[T <: ThriftEnum](using Quotes, Type[T]) = {
        import quotes.reflect.*

        val typSymbol = TypeRepr.of[T].typeSymbol

        val listOfValues: Expr[List[T]] = '{
            ${ Ref(typSymbol.companionModule).asExprOf[ThriftEnumObject[T]] }.list
        }

        val pkg = typSymbol.owner.fullName

        '{
            new com.gu.marley.AvroSerialisable[T] {
                val schema = com.gu.marley.AvroEnumSchema(
                    ${Expr(typSymbol.name)},
                    ${Expr(pkg)},
                    ${listOfValues}.map(com.gu.marley.enumsymbols.SnakesOnACamel.toSnake)
                )
                val schemaInstance = schema.apply()
    
                val valueMap: Map[T, Any] = Map(${listOfValues}.map(x => x ->
                  org.apache.avro.generic.GenericData.get.createEnum(
                      com.gu.marley.enumsymbols.SnakesOnACamel.toSnake(x), schemaInstance)
                ): _*)
    
                def writableValue(t: T) = valueMap(t)
    
                val readMap: Map[Any, T] = valueMap.map(_.swap)
    
                def read(x: Any) = readMap(x.toString)
            }
        }
    }

    def structMacro[T <: ThriftStruct](using Quotes, Type[T]): Expr[AvroSerialisable[T]] = {
        import quotes.reflect.*

        val typSymbol = TypeRepr.of[T].typeSymbol

        val apply = typSymbol.companionModule.declaredMethod("apply").head

        val fields = apply.paramSymss.head.map(param => (param.name, param.signature))

        val pkg = typSymbol.owner.fullName

        println(fields)

        ???
    }

    def unionMacro = ???

    private def implicitFor[T](typ: T)(using Quotes, Type[T]): Expr[AvroSerialisable[T]] = {
        import quotes.reflect.*

        val typRepr = TypeRepr.of[T]

        typRepr match {
            case AppliedType(Option, arg :: Nil) => ('{
                com.gu.marley.AvroSerialisable.OptionAvroSerialisable(${implicitFor(arg.asType)})
            }).asExprOf[AvroSerialisable[T]]
        }
    }

}
