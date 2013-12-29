package play2scalaz
package test

import scalaz.std.anyVal._
import scalaz.scalacheck.ScalazProperties._
import play.api.libs.json.JsResult

object JsResultSpec extends JsResultSpecBase{
  import play2scalaz._

  def jsErrorGen = jsErrorGenDefault

  checkAll(applicative.laws[JsResult])
  checkAll(plus.laws[JsResult])
  checkAll(equal.laws[JsResult[Int]])
}

