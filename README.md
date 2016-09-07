Marley [![Build Status](https://travis-ci.org/guardian/marley.svg?branch=master)](https://travis-ci.org/guardian/marley) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/marley_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gu/marley_2.11)
======

Marley is a library for serialising [Scrooge](https://twitter.github.io/scrooge/) generated classes representing 
[Thrift](http://thrift.apache.org/) objects as [Avro](https://avro.apache.org/) with minimal boilerplate.

Installation
------------

```scala
libraryDependencies ++= Seq(
  "com.gu" %% "marley" % "0.2.0"
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

Marley has been extracted from a project where it's been running successfully for a number of months.

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
