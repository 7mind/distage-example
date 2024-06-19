package leaderboard

import cats.effect.kernel.Sync
import org.scalacheck.Gen.Parameters
import org.scalacheck.{Arbitrary, Prop}

trait Rnd[F[_]] {
  def apply[A: Arbitrary]: F[A]
}

object Rnd {
  final class Impl[F[_]: Sync] extends Rnd[F] {
    override def apply[A: Arbitrary]: F[A] = {
      Sync[F].delay {
        val (p, s) = Prop.startSeed(Parameters.default)
        Arbitrary.arbitrary[A].pureApply(p, s)
      }
    }
  }
}
