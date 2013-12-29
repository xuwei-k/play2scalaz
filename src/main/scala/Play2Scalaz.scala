import scalaz._
import scalaz.Isomorphism._
import play.api.libs.json.{JsResult, JsSuccess, JsError, JsObject, JsArray, JsValue, OFormat}
import play.api.libs.functional.{
  InvariantFunctor => PlayInvariantFunctor,
  Functor => PlayFunctor,
  Applicative => PlayApplicative,
  Alternative => PlayAlternative,
  Monoid => PlayMonoid
}

package object play2scalaz{
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

  implicit val invariantFunctorIso: TypeclassIso[PlayInvariantFunctor, InvariantFunctor] =
    new TypeclassIso[PlayInvariantFunctor, InvariantFunctor](
      new (PlayInvariantFunctor ~~~> InvariantFunctor){
        def apply[M[_]](m: PlayInvariantFunctor[M]) =
          new InvariantFunctor[M] {
            def xmap[A, B](ma: M[A], f: A => B, g: B => A) =
              m.inmap(ma, f, g)
          }
      },
      new (InvariantFunctor ~~~> PlayInvariantFunctor){
        def apply[M[_]](m: InvariantFunctor[M]) =
          new PlayInvariantFunctor[M] {
            def inmap[A, B](ma: M[A], f: A => B, g: B => A) =
              m.xmap(ma, f, g)
          }
      }
    )

  implicit val functorIso: TypeclassIso[PlayFunctor, Functor] =
    new TypeclassIso[PlayFunctor, Functor](
      new (PlayFunctor ~~~> Functor){
        def apply[M[_]](m: PlayFunctor[M]) =
          new Functor[M] {
            override def map[A, B](ma: M[A])(f: A => B) =
              m.fmap(ma, f)
          }
      },
      new (Functor ~~~> PlayFunctor){
        def apply[M[_]](m: Functor[M]) =
          new PlayFunctor[M] {
            override def fmap[A, B](ma: M[A], f: A => B) =
              m.map(ma)(f)
          }
      }
    )

  implicit val applicativeIso: TypeclassIso[PlayApplicative, Applicative] =
    new TypeclassIso[PlayApplicative, Applicative](
      new (PlayApplicative ~~~> Applicative){
        def apply[M[_]](m: PlayApplicative[M]) =
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
        def apply[M[_]](m: Applicative[M]) =
          new PlayApplicative[M] {
            def map[A, B](ma: M[A], f: A => B) =
              m.map(ma)(f)
            def pure[A](a: A) =
              m.point(a)
            def apply[A, B](f: M[A => B], ma: M[A]) =
              m.ap(ma)(f)
          }
      }
    )

  implicit val alternativeIso: TypeclassIso[PlayAlternative, Alternative] =
    new TypeclassIso[PlayAlternative, Alternative](
      new (PlayAlternative ~~~> Alternative){
        def apply[M[+_]](m: PlayAlternative[M]) =
          new Alternative[M]{
            def point[A](a: => A) =
              m.app.pure(a)
            def ap[A, B](fa: => M[A])(f: => M[A => B]) =
              m.app.apply(f, fa)
            override def map[A, B](fa: M[A])(f: A => B) =
              m.app.map(fa, f)
            def plus[A](a: M[A], b: => M[A]) =
              m.|(a, b)
            def empty[A] =
              m.empty
          }
      },
      new (Alternative ~~~> PlayAlternative){
        def apply[M[+_]](m: Alternative[M]) =
          new PlayAlternative[M]{
            def app =
              applicativeIso.from(m)
            def |[A, B >: A](a: M[A], b: M[B]) =
              m.plus[B](a, b)
            val empty =
              m.empty
          }
      }
    )

  implicit val jsResultInstance: Alternative[JsResult] =
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

  implicit class ScalazAlternaiveOps[F[_]](val self: Alternative[F]) extends AnyVal{
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

  implicit val jsObjectMonoid: Monoid[JsObject] =
    monoidIso.to(play.api.libs.json.Reads.JsObjectMonoid)

  implicit val jsArrayMonoid: Monoid[JsArray] =
    monoidIso.to(play.api.libs.json.Reads.JsArrayMonoid)

  implicit val jsObjectEqual: Equal[JsObject] =
    Equal.equalA[JsObject]

  implicit val jsArrayEqual: Equal[JsArray] =
    Equal.equalA[JsArray]

  implicit val oFormatInvariant: InvariantFunctor[OFormat] =
    invariantFunctorIso.to(play.api.libs.json.OFormat.invariantFunctorOFormat)
}

