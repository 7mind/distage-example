package leaderboard.services

import izumi.functional.bio.Monad2
import leaderboard.model.{QueryFailure, RankedProfile, UserId}
import leaderboard.repo.{Ladder, Profiles}

trait Ranks[F[_, _]] {
  def getRank(userId: UserId): F[QueryFailure, Option[RankedProfile]]
}

object Ranks {
  final class Impl[F[+_, +_]: Monad2](
    ladder: Ladder[F],
    profiles: Profiles[F],
  ) extends Ranks[F] {

    override def getRank(userId: UserId): F[QueryFailure, Option[RankedProfile]] = {
      for {
        maybeProfile <- profiles.getProfile(userId)
        scores       <- ladder.getScores
        res = for {
          profile <- maybeProfile
          rank     = scores.indexWhere(_._1 == userId) + 1
          score    = scores.find(_._1 == userId).map(_._2)
        } yield RankedProfile(
          name        = profile.name,
          description = profile.description,
          rank        = rank,
          score       = score.getOrElse(0),
        )
      } yield res
    }

  }
}
