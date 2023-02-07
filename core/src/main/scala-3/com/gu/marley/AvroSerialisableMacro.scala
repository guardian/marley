package com.gu.marley

import com.twitter.scrooge.{ThriftEnum, ThriftEnumObject}

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

    def unionMacro = ???

    def structMacro = ???

}
