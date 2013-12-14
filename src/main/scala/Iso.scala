package play2scalaz

final case class TypeclassIso[F[_[_]], G[_[_]]](
  to: F ~~~> G, from: G ~~~> F 
)

/** higher order functin */
abstract class ~~~>[F[_[_]], G[_[_]]] {
  def apply[M[_]](fm: F[M]): G[M]
}

