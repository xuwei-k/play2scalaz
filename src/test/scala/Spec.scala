package play2scalaz.test

import scalaz._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import org.scalacheck.{Arbitrary, Gen}
import scalaz.scalacheck.ScalazProperties._
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.scalacheck.ScalaCheckBinding.GenMonad.applySyntax._
import scalaz.std.anyVal._

abstract class SpecBase extends scalaz.SpecLite {

  protected final def gen[A: Arbitrary]: Gen[A] =
    implicitly[Arbitrary[A]].arbitrary

  protected def arb[A: Arbitrary]: Arbitrary[A] =
    implicitly[Arbitrary[A]]

}

abstract class JsResultSpecBase extends SpecBase {

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
      ^(Gen.alphaStr, gen[List[Int]])(ValidationError.apply)
    )

  protected def jsErrorGen: Gen[JsError]

  // incorrect too...
  protected final def jsErrorGenMerged: Gen[JsError] =
    gen[List[(JsPath, List[ValidationError])]].map{ e =>
      JsError() ++ new JsError(e) // https://github.com/playframework/playframework/blob/2.2.2-RC1/framework/src/play-json/src/main/scala/play/api/libs/json/JsResult.scala#L13
    }

  protected final def jsErrorGenDefault: Gen[JsError] =
    gen[List[(JsPath, List[ValidationError])]].map{ e =>
      new JsError(e)
    }

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

object SuccessExample extends JsResultSpecBase{
  import play2scalaz._

  def jsErrorGen = jsErrorGenDefault

  checkAll(applicative.laws[JsResult])
  checkAll(plus.laws[JsResult])
  checkAll(equal.laws[JsResult[Int]])
}

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

object JsValueSpec extends SpecBase{
  import play2scalaz._

  private[this] val jsValuePrimitivesArb: Arbitrary[JsValue] =
      Arbitrary(Gen.oneOf(
        Gen.const(JsNull),
        gen[String].map(JsUndefined.apply(_)),
        gen[Boolean].map(JsBoolean),
        gen[BigDecimal].map(JsNumber),
        gen[String].map(JsString)
      ))

  private[this] val jsObjectArb1: Arbitrary[JsObject] =
    Arbitrary(Gen.choose(0, 4).flatMap(n =>
      Gen.listOfN(
        n,
        Arbitrary.arbTuple2(
          arb[String], jsValuePrimitivesArb
        ).arbitrary
      ).map(JsObject)
    ))

  private[this] val jsArrayArb1: Arbitrary[JsArray] =
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

  checkAll("JsObject", monoid.laws[JsObject])
  checkAll("JsArray",  monoid.laws[JsArray])

  checkAll("JsObject", equal.laws[JsObject])
  checkAll("JsArray",  equal.laws[JsArray])
}

