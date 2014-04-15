package play2scalacheck

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json._
import play.api.data.validation.ValidationError

object Play2Arbitrary extends Play2Arbitrary

trait Play2Arbitrary{

  // `org.scalacheck.Arbitrary.arbBigDecimal` is not useful. because too big or too small
  val bigDecimalGen: Gen[BigDecimal] = Gen.choose(Long.MinValue, Long.MaxValue).map(BigDecimal(_))

  val jsValuePrimitivesArb: Arbitrary[JsValue] =
    Arbitrary(Gen.oneOf(
      Gen.value(JsNull),
      gen[String].map(JsUndefined.apply(_)),
      gen[Boolean].map(JsBoolean),
      bigDecimalGen.map(JsNumber),
      gen[String].map(JsString)
    ))

  val jsObjectArb1: Arbitrary[JsObject] =
    Arbitrary(Gen.choose(0, 4).flatMap(n =>
      Gen.listOfN(
        n,
        Arbitrary.arbTuple2(
          arb[String], jsValuePrimitivesArb
        ).arbitrary
      ).map(JsObject)
    ))

  private val jsArrayArb1: Arbitrary[JsArray] =
    Arbitrary(Gen.choose(0, 4).flatMap(n =>
      Gen.listOfN(n, jsValuePrimitivesArb.arbitrary).map(JsArray)
    ))

  implicit val jsValueArb: Arbitrary[JsValue] =
    Arbitrary(Gen.oneOf(
      jsValuePrimitivesArb.arbitrary,
      jsObjectArb1.arbitrary,
      jsArrayArb1.arbitrary
    ))

  implicit val jsObjectArb: Arbitrary[JsObject] =
    Arbitrary(Gen.choose(0, 4).flatMap(n =>
      Gen.listOfN(
        n,
        Arbitrary.arbTuple2(arb[String], jsValueArb).arbitrary
      ).map(JsObject)
    ))

  implicit val jsArrayArb: Arbitrary[JsArray] =
    Arbitrary(Gen.choose(0, 4).flatMap(n =>
      Gen.listOfN(n, jsValueArb.arbitrary).map(JsArray)
    ))

  implicit def jsResultArb[A](implicit A: Arbitrary[A]): Arbitrary[JsResult[A]] =
    Arbitrary(Gen.oneOf(
      (for{ a <- A.arbitrary; b <- gen[JsPath] } yield JsSuccess(a, b)),
      jsErrorGen
    ))

  final def gen[A: Arbitrary]: Gen[A] =
    implicitly[Arbitrary[A]].arbitrary

  final def arb[A: Arbitrary]: Arbitrary[A] =
    implicitly[Arbitrary[A]]

  implicit val pathNodeArb: Arbitrary[PathNode] =
    Arbitrary(Gen.oneOf(
      Gen.alphaStr.map(KeyPathNode),
      Gen.alphaStr.map(RecursiveSearch),
      gen[Int].map(IdxPathNode)
    ))

  implicit val jsPathArb: Arbitrary[JsPath] =
    Arbitrary(gen[List[PathNode]].map(JsPath.apply))

  implicit val validationErrorArb: Arbitrary[ValidationError] =
    Arbitrary(
      for {
        a <- Gen.alphaStr
        b <- gen[List[Int]]
      } yield ValidationError(a, b)
    )

  final val jsErrorGen: Gen[JsError] =
    gen[List[(JsPath, List[ValidationError])]].map{ e =>
      new JsError(e)
    }

}

