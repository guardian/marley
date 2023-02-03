package com.gu.marley

import com.twitter.scrooge.{ThriftEnum, ThriftEnumObject}

import scala.quoted.*

class AvroSerialisableMacro {
    def enumMacro[T <: ThriftEnum](using Quotes, Type[T]) = {
        import quotes.reflect.*

        val typSymbol = TypeRepr.of[T].typeSymbol
        val listMethod: Symbol = typSymbol.companionClass.declarations.find(_.name == "list").get

        def findCompanionOfThisOrParent(): Expr[ThriftEnumObject[T]] = Ref(typSymbol.companionModule)
          .select(listMethod).appliedToArgs(Nil) // TODO I'm not entirely sure if this is necessary...
          .asExprOf[ThriftEnumObject[T]]

        val listOfValues: Expr[List[T]] = '{
            ${ findCompanionOfThisOrParent() }.list
        }

        val pkg = typSymbol.owner.fullName

        '{
            new com.gu.marley.AvroSerialisable[T] {
            val schema = com.gu.marley.AvroEnumSchema(
                ${Expr(typSymbol.name)},
                ${Expr(pkg)},
                ${listOfValues}.map(_.name).map(com.gu.marley.enumsymbols.SnakesOnACamel.toSnake)
            )
            val schemaInstance = schema.apply()

            val valueMap: Map[T, Any] = Map(${listOfValues}.map(x => x ->
              org.apache.avro.generic.GenericData.get.createEnum(
                  com.gu.marley.enumsymbols.SnakesOnACamel.toSnake(x.name), schemaInstance)
            ): _*)

            def writableValue(t: T) = valueMap(t)

            val readMap: Map[Any, T] = Map(${listOfValues}.map(x =>
            com.gu.marley.enumsymbols.SnakesOnACamel.toSnake(x.name) -> x
            ): _*)

            def read(x: Any) = readMap(x.toString)
        }
        }
    }

    def structMacro = ???

    def unionMacro = ???
}
