package leaderboard

import leaderboard.model.*
import leaderboard.repo.{Ladder, Profiles}
import leaderboard.services.Ranks
import org.scalacheck.Arbitrary
import zio.{RIO, Task, ZIO}

object zioenv {

  object ladder extends Ladder[RIO[Ladder[Task], _]] {
    def submitScore(userId: UserId, score: Score): RIO[Ladder[Task], Unit] = ZIO.serviceWithZIO(_.submitScore(userId, score))
    def getScores: RIO[Ladder[Task], List[(UserId, Score)]]                = ZIO.serviceWithZIO(_.getScores)
  }

  object profiles extends Profiles[RIO[Profiles[Task], _]] {
    override def setProfile(userId: UserId, profile: UserProfile): RIO[Profiles[Task], Unit] = ZIO.serviceWithZIO(_.setProfile(userId, profile))
    override def getProfile(userId: UserId): RIO[Profiles[Task], Option[UserProfile]]        = ZIO.serviceWithZIO(_.getProfile(userId))
  }

  object ranks extends Ranks[RIO[Ranks[Task], _]] {
    override def getRank(userId: UserId): RIO[Ranks[Task], Option[RankedProfile]] = ZIO.serviceWithZIO(_.getRank(userId))
  }

  object rnd extends Rnd[RIO[Rnd[Task], _]] {
    override def apply[A: Arbitrary]: RIO[Rnd[Task], A] = ZIO.serviceWithZIO(_.apply[A])
  }

}
