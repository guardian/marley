Marley
======

Marley is a library for serialising [Scrooge](https://twitter.github.io/scrooge/) generated classes representing 
[Thrift](http://thrift.apache.org/) objects as [Avro](https://avro.apache.org/) with minimal boilerplate.

Example Usage
-------------

```scala
import com.gu.marley._

implicit val enumSer = AvroSerialisable.enum[ExampleEnum]
implicit val structSer = AvroSerialisable.struct[ExampleStruct]

AvroFile.write(Seq(struct))(file)

val readStructs: Iterable[ExampleStruct] = AvroFile.read[ExampleStruct](file)
```