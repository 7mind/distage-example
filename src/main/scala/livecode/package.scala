import cats.effect.{Async, Bracket, ConcurrentEffect, ContextShift, Sync, Timer}
import izumi.functional.bio.BIO
import izumi.functional.bio.BIO._
import izumi.functional.bio.BIO.catz._

package object livecode {
  type Ref[F[_], A] = cats.effect.concurrent.Ref[F, A]
  def Ref[F[+_, +_]: BIO, A](a: A): F[Nothing, cats.effect.concurrent.Ref[F[Nothing, ?], A]] = {
    cats.effect.concurrent.Ref.of[F[Throwable, ?], A](a).map(_.mapK(F.orTerminateK)).orTerminate
  }

  type Bracket2[F[_, _]]          = Bracket[F[Throwable, ?], Throwable]
  type Sync2[F[_, _]]             = Sync[F[Throwable, ?]]
  type Async2[F[_, _]]            = Async[F[Throwable, ?]]
  type ConcurrentEffect2[F[_, _]] = ConcurrentEffect[F[Throwable, ?]]
  type ContextShift2[F[_, _]]     = ContextShift[F[Throwable, ?]]
  type Timer2[F[_, _]]            = Timer[F[Throwable, ?]]
}
