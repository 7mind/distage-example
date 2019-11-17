import cats.effect.{Async, Bracket, ConcurrentEffect, ContextShift, Timer}

package object livecode {
  type Bracket2[F[_, _]]          = Bracket[F[Throwable, ?], Throwable]
  type Async2[F[_, _]]            = Async[F[Throwable, ?]]
  type ConcurrentEffect2[F[_, _]] = ConcurrentEffect[F[Throwable, ?]]
  type ContextShift2[F[_, _]]     = ContextShift[F[Throwable, ?]]
  type Timer2[F[_, _]]            = Timer[F[Throwable, ?]]
}
