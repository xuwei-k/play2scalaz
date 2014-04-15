package play2scalaz

import scalaz.InvariantFunctor
import play.api.libs.json.OFormat
import play.api.libs.functional.{
  InvariantFunctor => PlayInvariantFunctor
}

trait Play2ScalazBase {

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

  implicit val oFormatInvariant: InvariantFunctor[OFormat] =
    invariantFunctorIso.to(play.api.libs.json.OFormat.invariantFunctorOFormat)

}

