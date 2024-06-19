package leaderboard

import cats.effect.IO
import org.scalacheck.Gen.Parameters
import org.scalacheck.{Arbitrary, Prop}

trait Rnd {
  def apply[A: Arbitrary]: IO[A]
}

object Rnd {
  final class Impl extends Rnd {
    override def apply[A: Arbitrary]: IO[A] = {
      IO {
        val (p, s) = Prop.startSeed(Parameters.default)
        Arbitrary.arbitrary[A].pureApply(p, s)
      }
    }
  }
}
