package com.gu.marley.enumsymbols

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

class SnakesOnACamelTest extends AnyFlatSpec with Matchers {

  it should "transform camel case with first-letter upper to snake case" in {
    SnakesOnACamel.toSnake("AndroidNative123App") should be ("ANDROID_NATIVE123_APP")
  }

  it should "transform snake case to snake case with first-letter upper" in {
    SnakesOnACamel.toCamel("ANDROID_NATIVE123_APP") should be ("AndroidNative123App")
  }
}

