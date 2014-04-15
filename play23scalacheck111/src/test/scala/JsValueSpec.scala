package play2scalaz

import scalaz._
import play.api.libs.json._
import org.scalacheck.{Arbitrary, Gen}
import scalaz.scalacheck.ScalazProperties._
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.std.anyVal._
import Play2Scalaz._

object JsValueSpec extends scalaz.SpecLite {
  import play2scalacheck.Play2Arbitrary._

  checkAll("JsArray",  monoid.laws[JsArray])

  checkAll("JsObject", equal.laws[JsObject])
  checkAll("JsArray",  equal.laws[JsArray])
}

