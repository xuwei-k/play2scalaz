package play2scalaz

import play.api.libs.json._
import play2scalaz.Play2Scalaz._
import scalaprops._
import scalaz._
import scalaz.std.anyVal._
import play2scalaz.PlayJsonGen._

object Play2Scalaprops extends Scalaprops {

  private[this] implicit def readsEqual[A: Equal]: Equal[Reads[A]] =
    Equal.equal{(a, b) =>
      val jsons = PlayJsonGen.jsValueGen.samples(listSize = 10, size = 10, seed = System.currentTimeMillis())
      jsons.forall(j => Equal[JsResult[A]].equal(a.reads(j), b.reads(j)))
    }

  private[this] implicit def owritesEqual[A: Equal: Gen]: Equal[OWrites[A]] =
    Equal.equal{(a, b) =>
      val jsons = Gen[A].samples(listSize = 10, size = 10, seed = System.currentTimeMillis())
      jsons.forall(j => Equal[JsObject].equal(a.writes(j), b.writes(j)))
    }

  private[this] implicit def oformatEqual[A: Equal: Gen]: Equal[OFormat[A]] = {
    import scalaz.std.tuple._
    Equal.equalBy { format =>
      (format: OWrites[A], format: Reads[A])
    }
  }

  // Monoid[JsObject] does not satisfy associative law
  // counter example
  //
  // Json.obj("key" -> Json.obj("x" -> 1))
  // Json.obj("key" -> 2)
  // Json.obj("key" -> Json.obj("y" -> 3))

  val testJsObject = scalazlaws.equal.all[JsObject]

  val testJsArray = Properties.list(
    scalazlaws.monoid.all[JsArray],
    scalazlaws.equal.all[JsArray]
  )

  val testJsResult = Properties.list(
    // scalazlaws.applicativePlus.all[JsResult], // does not satisfy laws
    scalazlaws.applicative.all[JsResult],
    scalazlaws.plus.all[JsResult],
    scalazlaws.equal.all[JsResult[Int]]
  ).andThenParam(Param.maxSize(5))

  val testReads = Properties.list(
    // does not satisfy plugEmpty laws
    scalazlaws.applicative.all[Reads],
    scalazlaws.plus.all[Reads]
  ).andThenParam(Param.maxSize(5))

  val testOWrites =
    scalazlaws.contravariant.all[OWrites]

  val testOFormat =
    scalazlaws.invariantFunctor.all[OFormat].andThenParam(Param.maxSize(5))

}
