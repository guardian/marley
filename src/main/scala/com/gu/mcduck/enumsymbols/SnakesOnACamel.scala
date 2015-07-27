package com.gu.mcduck.enumsymbols

import org.parboiled2._
import org.parboiled2.CharPredicate._

object SnakesOnACamel {
  def toSnake(camel: String): String = new CamelToSnake(camel).camelName.run().get
  def toCamel(snake: String): String = new SnakeToCamel(snake).snakeName.run().get
}

class CamelToSnake(val input: ParserInput) extends Parser {
  def camelName = rule { oneOrMore(camelWordAsSnake) ~> (_.mkString("_")) }
  def camelWordAsSnake = rule { capture(camelWord) ~> (_.toUpperCase) }
  def camelWord = rule { UpperAlpha ~ oneOrMore(LowerAlpha ++ Digit) }
}

class SnakeToCamel(val input: ParserInput) extends Parser {
  def snakeName = rule { oneOrMore(word).separatedBy("_") ~> (_.mkString) }
  def word = rule { capture(oneOrMore(AlphaNum)) ~> (_.toLowerCase.capitalize) }
}
