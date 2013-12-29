package play2scalaz
package test

import scalaz._, std.anyVal._, Isomorphism._
import org.scalacheck.{Arbitrary, Gen}
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.scalacheck.ScalazProperties._
import play.api.libs.json.{JsResult, Reads, JsValue, OWrites, JsObject, OFormat}

sealed trait LowPriorityReadsImplicits extends Play2Arbitrary{

  val readsKleisliIso: Reads <~> ({type λ[α] = Kleisli[JsResult, JsValue, α]})#λ =
    new IsoFunctorTemplate[Reads, ({type λ[α] = Kleisli[JsResult, JsValue, α]})#λ]{
      def from[A](ga: Kleisli[JsResult, JsValue, A]) =
        Reads(ga.run)
      def to[A](fa: Reads[A]) =
        Kleisli(fa.reads)
    }

  implicit def readsArb[A: Arbitrary]: Arbitrary[Reads[A]] = {
    import SuccessExample.jsResultArb1
    import scalaz.scalacheck.ScalazArbitrary.KleisliArbitrary
    Functor[Arbitrary].map(
      arb[Kleisli[JsResult, JsValue, A]]
    )(readsKleisliIso.from.apply)
  }

}

object ReadsSpec extends scalaz.SpecLite with LowPriorityReadsImplicits{

  implicit val intReadsArb: Arbitrary[Reads[Int]] =
    Arbitrary(Gen.oneOf(
      Gen.const(Reads.IntReads),
      readsArb[Int].arbitrary
    ))

  implicit val intReadsEqual: Equal[Reads[Int]] =
    Equal.equal{(a, b) =>
      val jsons = Iterator.continually(JsValueSpec.jsValuePrimitivesArb.arbitrary.sample).flatten.take(30).toList
      jsons.forall(j => Equal[JsResult[Int]].equal(a.reads(j), b.reads(j)))
    }

  checkAll(applicative.laws[Reads])
  checkAll(plus.laws[Reads])

}

object OWritesSpec extends scalaz.SpecLite with Play2Arbitrary{

  implicit def owritesArb[A: Arbitrary]: Arbitrary[OWrites[A]] = {
    import JsValueSpec.jsObjectArb
    Functor[Arbitrary].map(implicitly[Arbitrary[A => JsObject]])(OWrites.apply[A])
  }

  implicit def owritesEqual[A: Equal: Arbitrary]: Equal[OWrites[A]] =
    Equal.equal{(a, b) =>
      val jsons = Iterator.continually(implicitly[Arbitrary[A]].arbitrary.sample).flatten.take(30).toList
      jsons.forall(j => Equal[JsObject].equal(a.writes(j), b.writes(j)))
    }

  checkAll(contravariant.laws[OWrites])

}

object OFormatSpec extends scalaz.SpecLite{
  import OWritesSpec._
  import ReadsSpec._

  implicit def oFormatArb[A: Arbitrary]: Arbitrary[OFormat[A]] =
    Apply[Arbitrary].apply2(
      implicitly[Arbitrary[Reads[A]]],
      implicitly[Arbitrary[OWrites[A]]]
    )(OFormat.apply[A])

  implicit def oFormatIntEqual: Equal[OFormat[Int]] =
    Equal.equal{(a, b) =>
      Equal[OWrites[Int]].equal(a, b) && Equal[Reads[Int]].equal(a, b)
    }

  checkAll(invariantFunctor.laws[OFormat])
}

