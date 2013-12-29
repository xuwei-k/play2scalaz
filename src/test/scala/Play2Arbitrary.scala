package play2scalaz.test

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json._
import play.api.data.validation.ValidationError
import scalaz.scalacheck.ScalaCheckBinding.GenMonad.applySyntax._

object Play2Arbitrary extends Play2Arbitrary

trait Play2Arbitrary{

  final def gen[A: Arbitrary]: Gen[A] =
    implicitly[Arbitrary[A]].arbitrary

  final def arb[A: Arbitrary]: Arbitrary[A] =
    implicitly[Arbitrary[A]]

  implicit def pathNodeArb: Arbitrary[PathNode] =
    Arbitrary(Gen.oneOf(
      Gen.alphaStr.map(KeyPathNode),
      Gen.alphaStr.map(RecursiveSearch),
      gen[Int].map(IdxPathNode)
    ))

  implicit def jsPathArb: Arbitrary[JsPath] =
    Arbitrary(gen[List[PathNode]].map(JsPath.apply))

  implicit def validationErrorArb: Arbitrary[ValidationError] =
    Arbitrary(
      ^(Gen.alphaStr, gen[List[Int]])(ValidationError.apply)
    )

  // incorrect too...
  final def jsErrorGenMerged: Gen[JsError] =
    gen[List[(JsPath, List[ValidationError])]].map{ e =>
      JsError() ++ new JsError(e) // https://github.com/playframework/playframework/blob/2.2.2-RC1/framework/src/play-json/src/main/scala/play/api/libs/json/JsResult.scala#L13
    }

  final def jsErrorGenDefault: Gen[JsError] =
    gen[List[(JsPath, List[ValidationError])]].map{ e =>
      new JsError(e)
    }

}


