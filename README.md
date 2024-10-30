Marley [![marley Scala version support](https://index.scala-lang.org/guardian/marley/marley/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/guardian/marley/marley) [![Release](https://github.com/guardian/marley/actions/workflows/release.yml/badge.svg)](https://github.com/guardian/marley/actions/workflows/release.yml)
======

Marley is a library for serialising [Scrooge](https://twitter.github.io/scrooge/) generated classes representing 
[Thrift](http://thrift.apache.org/) objects as [Avro](https://avro.apache.org/) with minimal boilerplate.

Installation
------------

```scala
libraryDependencies ++= Seq(
  "com.gu" %% "marley" % "0.2.16"
)
``` 

Usage
-----

```scala
import com.gu.marley._

implicit val enumSer = AvroSerialisable.enum[ExampleEnum]
implicit val structSer = AvroSerialisable.struct[ExampleStruct]

AvroFile.write(Seq(struct))(file)

val readStructs: Iterable[ExampleStruct] = AvroFile.read[ExampleStruct](file)
```

Is this usable in production?
-----------------------------

Marley has been used successfully in the Guardian's realtime analytics system
Ophan for many years.

Marley is macro based and doesn't perform any runtime reflection. I haven't 
done any micro-benchmarking, but it's currently serialising more than 5000 
reasonably complicated messages/minute on an EC2 t2.medium without breaking 
a sweat.

License
-------

Marley is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0) (the "License"); 
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an 
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific 
language governing permissions and limitations under the License.
