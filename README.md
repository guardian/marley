Marley
======

Marley is a library for serialising [Scrooge]() generated classes representing [Thrift]() objects as [Avro]() with 
minimal boilerplate.

Example Usage
-------------

```scala
import com.gu.mcduck._

implicit val enumSer = AvroSerialisable.enum[ExampleEnum]
implicit val structSer = AvroSerialisable.struct[ExampleStruct]

AvroFile.write(Seq(struct))(file)
```