package play2scalaz

import scalaz._
import scalaz.Isomorphism._
import play.api.libs.json.{
  JsResult, JsSuccess, JsError, JsObject, JsArray, JsValue, OFormat, Writes, OWrites, Reads
}
import play.api.libs.functional.{
  Functor => PlayFunctor,
  ContravariantFunctor => PlayContravariant,
  InvariantFunctor => PlayInvariantFunctor,
  Applicative => PlayApplicative,
  Alternative => PlayAlternative,
  Monoid => PlayMonoid
}

final case class TypeclassIso[F[_[_]], G[_[_]]](
  to: F ~~~> G, from: G ~~~> F
)

/** higher order function */
abstract class ~~~>[F[_[_]], G[_[_]]] {
  def apply[M[_]](implicit fm: F[M]): G[M]
}

object Play2Scalaz {

  implicit val invariantFunctorIso: TypeclassIso[PlayInvariantFunctor, InvariantFunctor] =
    new TypeclassIso[PlayInvariantFunctor, InvariantFunctor](
      new (PlayInvariantFunctor ~~~> InvariantFunctor){
        def apply[M[_]](implicit m: PlayInvariantFunctor[M]) =
          new InvariantFunctor[M] {
            def xmap[A, B](ma: M[A], f: A => B, g: B => A) =
              m.inmap(ma, f, g)
          }
      },
      new (InvariantFunctor ~~~> PlayInvariantFunctor){
        def apply[M[_]](implicit m: InvariantFunctor[M]) =
          new PlayInvariantFunctor[M] {
            def inmap[A, B](ma: M[A], f: A => B, g: B => A) =
              m.xmap(ma, f, g)
          }
      }
    )

  implicit val oFormatInvariant: InvariantFunctor[OFormat] =
    invariantFunctorIso.to(play.api.libs.json.OFormat.invariantFunctorOFormat)

  implicit val monoidIso: PlayMonoid <~> Monoid =
    new IsoFunctorTemplate[PlayMonoid, Monoid]{
      def from[A](ga: Monoid[A]) =
        new PlayMonoid[A]{
          def append(a1: A, a2: A) =
            ga.append(a1, a2)
          val identity = ga.zero
        }
      def to[A](ga: PlayMonoid[A]) =
        new Monoid[A]{
          def append(a1: A, a2: => A) =
            ga.append(a1, a2)
          val zero = ga.identity
        }
    }

  implicit val contravariantIso: TypeclassIso[PlayContravariant, Contravariant] =
    new TypeclassIso[PlayContravariant, Contravariant](
      new (PlayContravariant ~~~> Contravariant){
        def apply[M[_]](implicit m: PlayContravariant[M]) =
          new Contravariant[M] {
            def contramap[A, B](ma: M[A])(f: B => A) =
              m.contramap(ma, f)
          }
      },
      new (Contravariant ~~~> PlayContravariant){
        def apply[M[_]](implicit m: Contravariant[M]) =
          new PlayContravariant[M] {
            def contramap[A, B](ma: M[A], f: B => A) =
              m.contramap(ma)(f)
          }
      }
    )

  implicit val functorIso: TypeclassIso[PlayFunctor, Functor] =
    new TypeclassIso[PlayFunctor, Functor](
      new (PlayFunctor ~~~> Functor){
        def apply[M[_]](implicit m: PlayFunctor[M]) =
          new Functor[M] {
            override def map[A, B](ma: M[A])(f: A => B) =
              m.fmap(ma, f)
          }
      },
      new (Functor ~~~> PlayFunctor){
        def apply[M[_]](implicit m: Functor[M]) =
          new PlayFunctor[M] {
            override def fmap[A, B](ma: M[A], f: A => B) =
              m.map(ma)(f)
          }
      }
    )

  implicit val applicativeIso: TypeclassIso[PlayApplicative, Applicative] =
    new TypeclassIso[PlayApplicative, Applicative](
      new (PlayApplicative ~~~> Applicative){
        def apply[M[_]](implicit m: PlayApplicative[M]) =
          new Applicative[M] {
            def point[A](a: => A) =
              m.pure(a)
            def ap[A, B](fa: => M[A])(f: => M[A => B]) =
              m.apply(f, fa)
            override def map[A, B](fa: M[A])(f: A => B) =
              m.map(fa, f)
          }
      },
      new (Applicative ~~~> PlayApplicative){
        def apply[M[_]](implicit m: Applicative[M]) =
          new PlayApplicative[M] {
            def map[A, B](ma: M[A], f: A => B) =
              m.map(ma)(f)
            override def pure[A](a: A) =
              m.point(a)
            def pure[A](a: => A) =
              m.point(a)
            def apply[A, B](f: M[A => B], ma: M[A]) =
              m.ap(ma)(f)
          }
      }
    )

  implicit val alternativeIso: TypeclassIso[PlayAlternative, ApplicativePlus] =
    new TypeclassIso[PlayAlternative, ApplicativePlus](
      new (PlayAlternative ~~~> ApplicativePlus){
        def apply[M[_]](implicit m: PlayAlternative[M]) =
          new ApplicativePlus[M]{
            def point[A](a: => A) =
              m.app.pure(a)
            def ap[A, B](fa: => M[A])(f: => M[A => B]) =
              m.app.apply(f, fa)
            override def map[A, B](fa: M[A])(f: A => B) =
              m.app.map(fa, f)
            def plus[A](a: M[A], b: => M[A]) =
              m.|(a, b)
            def empty[A] =
              m.empty.asInstanceOf[M[A]]
          }
      },
      new (ApplicativePlus ~~~> PlayAlternative){
        def apply[M[_]](implicit m: ApplicativePlus[M]) =
          new PlayAlternative[M]{
            def app =
              applicativeIso.from(m)
            def |[A, B >: A](a: M[A], b: M[B]) =
              m.plus[B](a.asInstanceOf[M[B]], b)
            val empty =
              m.empty
          }
      }
    )

  /**
   * @note does not satisfy law
   */
  implicit val jsResultInstance: ApplicativePlus[JsResult] =
    implicitly[PlayAlternative[JsResult]].toScalaz


  implicit class PlayFunctorOps[F[_]](val self: PlayFunctor[F]) extends AnyVal{
    def toScalaz: Functor[F] = functorIso.to(self)
  }

  implicit class PlayApplicativeOps[F[_]](val self: PlayApplicative[F]) extends AnyVal{
    def toScalaz: Applicative[F] = applicativeIso.to(self)
  }

  implicit class PlayAlternativeOps[F[+_]](val self: PlayAlternative[F]) extends AnyVal{
    def toScalaz: ApplicativePlus[F] = alternativeIso.to(self)
  }


  implicit class ScalazFunctorOps[F[_]](val self: Functor[F]) extends AnyVal{
    def toPlay: PlayFunctor[F] = functorIso.from(self)
  }

  implicit class ScalazApplicativeOps[F[_]](val self: Applicative[F]) extends AnyVal{
    def toPlay: PlayApplicative[F] = applicativeIso.from(self)
  }

  implicit class ScalazAlternaiveOps[F[_]](val self: ApplicativePlus[F]) extends AnyVal{
    def toPlay: PlayAlternative[F] = alternativeIso.from(self)
  }

  implicit def jsResultEqual[A](implicit A: Equal[A]): Equal[JsResult[A]] =
    Equal.equal{
      case (a: JsSuccess[_], b: JsSuccess[_]) =>
        // does not satisfy the laws if compare path
        A.equal(a.get, b.get) // && a.path == b.path
      case (a: JsError, b: JsError) =>
        // need `toSet` !!!
        a.errors.toSet == b.errors.toSet
      case (_, _) => false
    }

  val readsKleisliIso: Reads <~> ({type λ[α] = Kleisli[JsResult, JsValue, α]})#λ =
    new IsoFunctorTemplate[Reads, ({type λ[α] = Kleisli[JsResult, JsValue, α]})#λ]{
      def from[A](ga: Kleisli[JsResult, JsValue, A]) =
        Reads(ga.run)
      def to[A](fa: Reads[A]) =
        Kleisli(fa.reads)
    }

  val writesFunction1Iso: Writes <~> ({type λ[α] = α => JsValue})#λ =
    new IsoFunctorTemplate[Writes, ({type λ[α] = α => JsValue})#λ]{
      def from[A](ga: A => JsValue) =
        Writes(ga)
      def to[A](fa: Writes[A]) =
        fa.writes _
    }

  val owritesFunction1Iso: OWrites <~> ({type λ[α] = α => JsObject})#λ =
    new IsoFunctorTemplate[OWrites, ({type λ[α] = α => JsObject})#λ]{
      def from[A](ga: A => JsObject) =
        OWrites(ga)
      def to[A](fa: OWrites[A]) =
        fa.writes _
    }

  /**
   * @note does not satisfy associative laws
   * @see [[https://gist.github.com/xuwei-k/9a306226b04f9214f742]]
   */
  implicit val jsObjectMonoid: Monoid[JsObject] =
    monoidIso.to(play.api.libs.json.Reads.JsObjectMonoid)

  implicit val jsArrayMonoid: Monoid[JsArray] =
    monoidIso.to(play.api.libs.json.Reads.JsArrayMonoid)

  implicit val jsObjectEqual: Equal[JsObject] =
    Equal.equalA[JsObject]

  implicit val jsArrayEqual: Equal[JsArray] =
    Equal.equalA[JsArray]

  /**
   * `FunctionalCanBuild` + `Contravariant` = `scalaz.Divide`
   *
   * @see [[https://github.com/playframework/playframework/blob/2.5.0/framework/src/play-json/src/main/scala/play/api/libs/json/Writes.scala#L66-L70]]
   */
  implicit val oWritesDivisible: Divisible[OWrites] =
    new Divisible[OWrites] {
      override def contramap[A, B](r: OWrites[A])(f: B => A) =
        OWrites.contravariantfunctorOWrites.contramap(r, f)

      override def divide2[A, B, C](fa: => OWrites[A], fb: => OWrites[B])(f: C => (A, B)) =
        OWrites[C]{ c =>
          val x = f(c)
          fa.writes(x._1) deepMerge fb.writes(x._2)
        }

      private[this] val emptyJsObject = JsObject(Nil)
      def conquer[A] = OWrites[A](_ => emptyJsObject)
    }

  /**
   * @note does not satisfy laws
   */
  implicit val readsApplicativePlus: ApplicativePlus[Reads] =
    alternativeIso.to(play.api.libs.json.Reads.alternative)

}
