package com.gu.marley.enumsymbols

import org.scalatest.{Matchers, FlatSpec}

class SnakesOnACamelTest extends FlatSpec with Matchers {

  it should "transform camel case with first-letter upper to snake case" in {
    SnakesOnACamel.toSnake("AndroidNative123App") should be ("ANDROID_NATIVE123_APP")
  }

  it should "transform snake case to snake case with first-letter upper" in {
    SnakesOnACamel.toCamel("ANDROID_NATIVE123_APP") should be ("AndroidNative123App")
  }
}

