package play2scalaz

import play.api.libs.json._
import org.scalacheck.{Arbitrary, Gen}
import scalaz.scalacheck.ScalazProperties._
import Play2Scalaz._

object JsValueSpec extends scalaz.SpecLite {
  import play2scalacheck.Play2Arbitrary._

  checkAll("JsArray",  monoid.laws[JsArray])

  checkAll("JsObject", equal.laws[JsObject])
  checkAll("JsArray",  equal.laws[JsArray])
}

