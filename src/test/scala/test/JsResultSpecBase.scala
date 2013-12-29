package play2scalaz.test

import scalaz._
import play.api.libs.json._
import org.scalacheck.{Arbitrary, Gen}
import scalaz.scalacheck.ScalazProperties._
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.scalacheck.ScalaCheckBinding.GenMonad.applySyntax._
import scalaz.std.anyVal._

abstract class JsResultSpecBase extends scalaz.SpecLite with Play2Arbitrary{

  protected def jsErrorGen: Gen[JsError]

  implicit def jsResultArb1[A: Arbitrary]: Arbitrary[JsResult[A]] =
    Arbitrary(Gen.oneOf(
      ^(gen[A], gen[JsPath])(JsSuccess.apply),
      jsErrorGen
    ))

  def jsResultIncorrectEqual1[A](implicit A: Equal[A]): Equal[JsResult[A]] =
    Equal.equal{
      case (a: JsSuccess[_], b: JsSuccess[_]) =>
        A.equal(a.get, b.get) && a.path == b.path
      case (a: JsError, b: JsError) =>
        a.errors.toSet == b.errors.toSet
      case (_, _) => false
    }

  def jsResultIncorrectEqual2[A](implicit A: Equal[A]): Equal[JsResult[A]] =
    Equal.equal{
      case (a: JsSuccess[_], b: JsSuccess[_]) =>
        A.equal(a.get, b.get)
      case (a: JsError, b: JsError) =>
        a == b
      case (_, _) => false
    }

}

