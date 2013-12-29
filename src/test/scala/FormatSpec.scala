package play2scalaz
package test

import scalaz._, std.anyVal._, Isomorphism._
import org.scalacheck.{Arbitrary, Gen}
import scalaz.scalacheck.ScalaCheckBinding._
import scalaz.scalacheck.ScalazProperties._
import play.api.libs.json.{JsResult, Reads, JsValue}

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

