package leaderboard

import leaderboard.model.*
import leaderboard.repo.{Ladder, Profiles}
import leaderboard.services.Ranks
import org.scalacheck.Arbitrary
import zio.{IO, URIO, ZIO}

object zioenv {

  object ladder extends Ladder[ZIO[Ladder[IO], _, _]] {
    def submitScore(userId: UserId, score: Score): ZIO[Ladder[IO], QueryFailure, Unit] = ZIO.serviceWithZIO(_.submitScore(userId, score))
    def getScores: ZIO[Ladder[IO], QueryFailure, List[(UserId, Score)]]                = ZIO.serviceWithZIO(_.getScores)
  }

  object profiles extends Profiles[ZIO[Profiles[IO], _, _]] {
    override def setProfile(userId: UserId, profile: UserProfile): ZIO[Profiles[IO], QueryFailure, Unit] = ZIO.serviceWithZIO(_.setProfile(userId, profile))
    override def getProfile(userId: UserId): ZIO[Profiles[IO], QueryFailure, Option[UserProfile]]        = ZIO.serviceWithZIO(_.getProfile(userId))
  }

  object ranks extends Ranks[ZIO[Ranks[IO], _, _]] {
    override def getRank(userId: UserId): ZIO[Ranks[IO], QueryFailure, Option[RankedProfile]] = ZIO.serviceWithZIO(_.getRank(userId))
  }

  object rnd extends Rnd[ZIO[Rnd[IO], _, _]] {
    override def apply[A: Arbitrary]: URIO[Rnd[IO], A] = ZIO.serviceWithZIO(_.apply[A])
  }

}
