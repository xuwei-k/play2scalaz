package play2scalaz.test

import scalaz._
import play.api.libs.json._
import org.scalacheck.{Arbitrary, Gen}
import scalaz.scalacheck.ScalazProperties._
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.std.anyVal._

object JsValueSpec extends scalaz.SpecLite{
  import play2scalaz._
  import Play2Arbitrary.{gen, arb}

  // `org.scalacheck.Arbitrary.arbBigDecimal` is not useful. because too big or too small
  val bigDecimalGen: Gen[BigDecimal] = Gen.choose(Long.MinValue, Long.MaxValue).map(BigDecimal(_))

  def jsValuePrimitivesArb: Arbitrary[JsValue] =
    Arbitrary(Gen.oneOf(
      Gen.const(JsNull),
      gen[String].map(JsUndefined.apply(_)),
      gen[Boolean].map(JsBoolean),
      bigDecimalGen.map(JsNumber),
      gen[String].map(JsString)
    ))

  def jsObjectArb1: Arbitrary[JsObject] =
    Arbitrary(Gen.choose(0, 4).flatMap(n =>
      Gen.listOfN(
        n,
        Arbitrary.arbTuple2(
          arb[String], jsValuePrimitivesArb
        ).arbitrary
      ).map(JsObject)
    ))

  private def jsArrayArb1: Arbitrary[JsArray] =
    Arbitrary(Gen.choose(0, 4).flatMap(n =>
      Gen.listOfN(n, jsValuePrimitivesArb.arbitrary).map(JsArray)
    ))

  implicit def jsValueArb: Arbitrary[JsValue] =
    Arbitrary(Gen.oneOf(
      jsValuePrimitivesArb.arbitrary,
      jsObjectArb1.arbitrary,
      jsArrayArb1.arbitrary
    ))

  implicit def jsObjectArb: Arbitrary[JsObject] =
    Arbitrary(Gen.choose(0, 4).flatMap(n =>
      Gen.listOfN(
        n,
        Arbitrary.arbTuple2(arb[String], jsValueArb).arbitrary
      ).map(JsObject)
    ))

  implicit def jsArrayArb: Arbitrary[JsArray] =
    Arbitrary(Gen.choose(0, 4).flatMap(n =>
      Gen.listOfN(n, jsValueArb.arbitrary).map(JsArray)
    ))

  checkAll("JsArray",  monoid.laws[JsArray])

  checkAll("JsObject", equal.laws[JsObject])
  checkAll("JsArray",  equal.laws[JsArray])
}

