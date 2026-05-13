package leaderboard.repo

import distage.Lifecycle
import izumi.functional.bio.{Applicative2, F, Primitives2}
import leaderboard.model.{QueryFailure, Score, UserId}

trait Ladder[F[_, _]] {
  def submitScore(userId: UserId, score: Score): F[QueryFailure, Unit]
  def getScores: F[QueryFailure, List[(UserId, Score)]]
}

object Ladder {
  // In-memory dummy used by both JVM tests and the in-browser simulation.
  final class Dummy[F[+_, +_]: Applicative2: Primitives2]
    extends Lifecycle.LiftF[F[Nothing, _], Ladder[F]](for {
      state <- F.mkRef(Map.empty[UserId, Score])
    } yield {
      new Ladder[F] {
        override def submitScore(userId: UserId, score: Score): F[Nothing, Unit] =
          state.update_(_ + (userId -> score))

        override def getScores: F[Nothing, List[(UserId, Score)]] =
          state.get.map(_.toList.sortBy(_._2)(Ordering[Score].reverse))
      }
    })
}
