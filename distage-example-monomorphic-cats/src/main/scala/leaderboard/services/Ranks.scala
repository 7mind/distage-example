package leaderboard.services

import cats.effect.IO
import leaderboard.model.{RankedProfile, UserId}
import leaderboard.repo.{Ladder, Profiles}

trait Ranks {
  def getRank(userId: UserId): IO[Option[RankedProfile]]
}

object Ranks {
  final class Impl(
    ladder: Ladder,
    profiles: Profiles,
  ) extends Ranks {

    override def getRank(userId: UserId): IO[Option[RankedProfile]] =
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
