package play2scalaz

import scalaz.std.anyVal._
import scalaz.scalacheck.ScalazProperties._
import play.api.libs.json.JsResult

object JsResultSpec extends scalaz.SpecLite {
  import play2scalaz.Play2Scalaz._
  import play2scalacheck.Play2Arbitrary._

  checkAll(applicative.laws[JsResult])
  checkAll(plus.laws[JsResult])
  checkAll(equal.laws[JsResult[Int]])
}

