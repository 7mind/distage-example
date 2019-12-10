package example.repo

import example.model.{QueryFailure, RankedProfile, UserId}
import izumi.functional.bio.BIOMonad

trait Ranks[F[_, _]] {
  def getRank(userId: UserId): F[QueryFailure, Option[RankedProfile]]
}

object Ranks {
  final class Impl[F[+_, +_]: BIOMonad](
    ladder: Ladder[F],
    profiles: Profiles[F],
  ) extends Ranks[F] {

    override def getRank(userId: UserId): F[QueryFailure, Option[RankedProfile]] = {
      for {
        maybeProfile <- profiles.getProfile(userId)
        scores       <- ladder.getScores
        res = for {
          profile <- maybeProfile
          rank    = scores.indexWhere(_._1 == userId) + 1
          score   <- scores.find(_._1 == userId).map(_._2)
        } yield RankedProfile(
          name        = profile.name,
          description = profile.description,
          rank        = rank,
          score       = score,
        )
      } yield res
    }

  }
}
