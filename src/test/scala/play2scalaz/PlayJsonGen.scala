package play2scalaz

import play.api.libs.json._
import play2scalaz.Play2Scalaz._
import scalaprops._
import scalaprops.ScalapropsScalaz._
import scalaz._

object PlayJsonGen {

  val jsValuePrimitivesGen: Gen[JsValue] =
    Gen.oneOf(
      Gen.value(JsNull),
      Gen[Boolean].map(JsBoolean),
      Gen[Long].map(n => JsNumber(BigDecimal(n))),
      Gen.alphaNumString.map(JsString)
    )

  private[this] def createJsObjectGen(valueGen: Gen[JsValue]): Gen[JsObject] =
    Gen.listOfN(
      4,
      Apply[Gen].tuple2(
        Gen.alphaNumString, valueGen
      )
    ).map(JsObject(_))


  val jsObjectGen1: Gen[JsValue] =
    createJsObjectGen(jsValuePrimitivesGen).map(identity)

  val jsArrayGen1: Gen[JsValue] =
    Gen.listOfN(4, jsValuePrimitivesGen).map(JsArray(_))

  implicit val jsValueGen: Gen[JsValue] =
    Gen.oneOf(
      jsValuePrimitivesGen,
      jsObjectGen1,
      jsArrayGen1
    )

  implicit val jsObjectGen: Gen[JsObject] =
    createJsObjectGen(jsValueGen)

  implicit val jsArrayGen: Gen[JsArray] =
    Gen.listOfN(4, jsValueGen).map(JsArray(_))

  implicit val pathNodeGen: Gen[PathNode] =
    Gen.oneOf(
      Gen.alphaString.map(KeyPathNode),
      Gen.alphaString.map(RecursiveSearch),
      Gen[Int].map(IdxPathNode)
    )

  implicit val jsPathGen: Gen[JsPath] =
    Gen[List[PathNode]].map(JsPath.apply)

  implicit val validationErrorGen: Gen[JsonValidationError] =
    Apply[Gen].apply2(
      Gen.alphaString,
      Gen[List[Int]]
    )(JsonValidationError(_, _))

  final def jsErrorGen[A]: Gen[JsResult[A]] =
    Gen[List[(JsPath, List[JsonValidationError])]].map{ e =>
      new JsError(e)
    }

  implicit def jsResultGen[A](implicit A: Gen[A]): Gen[JsResult[A]] =
    Gen.oneOf(
      Apply[Gen].apply2(A, Gen[JsPath])(JsSuccess(_, _)),
      jsErrorGen
    )

  implicit val jsValueCogen: Cogen[JsValue] =
    Cogen[String].contramap(_.toString)

  implicit def readsGen[A: Gen]: Gen[Reads[A]] =
    Gen[Kleisli[JsResult, JsValue, A]].map(readsKleisliIso.unlift[A].from)

  implicit def owritesGen[A: Cogen]: Gen[OWrites[A]] =
    Gen[A => JsObject].map(owritesFunction1Iso.unlift[A].from)

  implicit def oformatGen[A: Gen: Cogen]: Gen[OFormat[A]] =
    Apply[Gen].apply2(Gen[Reads[A]], Gen[OWrites[A]])(OFormat(_, _))

}
