package play2scalaz.failure_test

import play2scalaz.test._
import scalaz._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import org.scalacheck.{Arbitrary, Gen}
import scalaz.scalacheck.ScalazProperties._
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.scalacheck.ScalaCheckBinding.GenMonad.applySyntax._
import scalaz.std.anyVal._

object FailureExample1 extends JsResultSpecBase{
  import play2scalaz.{jsResultEqual => _, _}

  def jsErrorGen = jsErrorGenDefault

  override implicit def jsResultIncorrectEqual1[A: Equal] =
    super.jsResultIncorrectEqual2[A]

  checkAll(applicative.laws[JsResult])
  checkAll(plus.laws[JsResult])
}

object FailureExample2 extends JsResultSpecBase{
  import play2scalaz.{jsResultEqual => _, _}

  def jsErrorGen = jsErrorGenDefault

  override implicit def jsResultIncorrectEqual2[A: Equal] =
    super.jsResultIncorrectEqual2[A]

  checkAll(applicative.laws[JsResult])
  checkAll(plus.laws[JsResult])
}

object FailureExample3 extends JsResultSpecBase{
  import play2scalaz.{jsResultEqual => _, _}

  def jsErrorGen = jsErrorGenDefault

  override implicit def jsResultIncorrectEqual2[A: Equal] =
    super.jsResultIncorrectEqual2[A]

  override def jsResultArb1[A: Arbitrary] = ???

  implicit def jsResultArb2[A: Arbitrary]: Arbitrary[JsResult[A]] =
    Arbitrary(Gen.oneOf(
      ^(gen[A], gen[JsPath])(JsSuccess.apply),
      gen[List[(JsPath, List[ValidationError])]].map(e => JsError() ++ new JsError(e))
    ))

  checkAll(applicative.laws[JsResult])
  checkAll(plus.laws[JsResult])
}

object FailureExample4 extends JsResultSpecBase{
  import play2scalaz.{jsResultEqual => _, _}

  override implicit def jsResultIncorrectEqual2[A: Equal] =
    super.jsResultIncorrectEqual2[A]

  def jsErrorGen = jsErrorGenMerged

  checkAll(applicative.laws[JsResult])
  checkAll(plus.laws[JsResult])
}

object JsObjectFailure extends SpecLite{
  import JsValueSpec._
  import play2scalaz._

  checkAll(monoid.laws[JsObject])
}
