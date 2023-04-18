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

        val applyMethod = typSymbol.companionModule.declaredMethod("apply").head

        val fields = applyMethod.paramSymss.head.map(param => (param.name, param.signature))

        val pkg = typSymbol.owner.fullName

        println(applyMethod)
        println(fields)
        println(applyMethod.paramSymss.head)

        val fieldNamesWithImplicits: Seq[(String, Expr[AvroSerialisable[Any]])] = Seq.empty

        val fieldSchemas: Seq[Expr[(String, AvroSchema)]] =
            fieldNamesWithImplicits.map { case (fieldName: String, implicitSerialisable: Expr[AvroSerialisable[Any]]) =>
              '{
                  "Foo" -> (${implicitSerialisable}.schema : AvroSchema)
                  //"Foo" -> (${implicitSerialisable})
                  //${Expr(fieldName)}//  -> ${implicitSerialisable}.schema
              }
            }

//        val listOfValues: Expr[List[T]] = '{
//            ${
//                Ref(typSymbol.companionModule).asExprOf[ThriftEnumObject[T]]
//            }.list
//        }

        '{
            new com.gu.marley.AvroSerialisable[T] {
                val schema = com.gu.marley.AvroRecordSchema(
                    ${ Expr(typSymbol.name) },
                    ${ Expr(pkg) },
                    ${ Varargs(fieldSchemas) }: _*
                )
                val schemaInstance = schema.apply()


                def writableValue(t: T) = {
                    val foo: Seq[(String, Any)] = ${ Expr.ofSeq(
                        fieldNamesWithImplicits.map { case (fieldName, implicitSerialisable) =>
                            //'{${fieldName} -> ${implicitSerialisable}.writableValue(t.$fieldName)}
                            '{
                                ${Expr(fieldName)} -> ${ implicitSerialisable }.writableValue(${
                                    Select.unique(('t).asTerm, fieldName).asExpr
                                })
                            }
                        }
                    ) }

                    foo.foldLeft(
                        new org.apache.avro.generic.GenericRecordBuilder(schemaInstance)
                    ) { (b, nv) => b.set(nv._1, nv._2) }.build
                }

                def read(x: Any) = ???
            }
        }
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
