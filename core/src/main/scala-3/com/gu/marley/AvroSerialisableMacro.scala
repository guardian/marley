package com.gu.marley

import scala.quoted.*

class AvroSerialisableMacro {
    def enumMacro[T: Type](using Quotes) = {
        import quotes.reflect.*
        val typ = TypeRepr.of[T]

        def findCompanionOfThisOrParentWithMethod(method: String): Option[Symbol] =
            (for {
                baseClass <- typ.baseClasses
                list <- baseClass.companionClass.declarations.find(_.name == method)
            } yield list).headOption
        
        val listMethod = findCompanionOfThisOrParentWithMethod("list").getOrElse(
            report.errorAndAbort("Expected ThriftEnum companion to have method 'list'"))

        val pkg = typ.typeSymbol.owner.fullName

        '{
            new com.gu.marley.AvroSerialisable[T] {
            val schema = {
            com.gu.marley.AvroEnumSchema(
                ${Expr(typ.typeSymbol.name)},
                ${Expr(pkg)},
                ${listMethod}.map(_.name).map(com.gu.marley.enumsymbols.SnakesOnACamel.toSnake)
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
        }
    }

    def structMacro = ???

    def unionMacro = ???
}
