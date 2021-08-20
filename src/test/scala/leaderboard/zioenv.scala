package leaderboard

import leaderboard.model.*
import leaderboard.repo.{Ladder, Profiles, Ranks}
import org.scalacheck.Arbitrary
import zio.{Has, IO, URIO, ZIO}

object zioenv {

  object ladder extends Ladder[ZIO[LadderEnv, _, _]] {
    def submitScore(userId: UserId, score: Score): ZIO[LadderEnv, QueryFailure, Unit] = ZIO.accessM(_.get.submitScore(userId, score))
    def getScores: ZIO[LadderEnv, QueryFailure, List[(UserId, Score)]]                = ZIO.accessM(_.get.getScores)
  }

  object profiles extends Profiles[ZIO[ProfilesEnv, _, _]] {
    override def setProfile(userId: UserId, profile: UserProfile): ZIO[ProfilesEnv, QueryFailure, Unit] = ZIO.accessM(_.get.setProfile(userId, profile))
    override def getProfile(userId: UserId): ZIO[ProfilesEnv, QueryFailure, Option[UserProfile]]        = ZIO.accessM(_.get.getProfile(userId))
  }

  object ranks extends Ranks[ZIO[RanksEnv, _, _]] {
    override def getRank(userId: UserId): ZIO[RanksEnv, QueryFailure, Option[RankedProfile]] = ZIO.accessM(_.get.getRank(userId))
  }

  object rnd extends Rnd[ZIO[RndEnv, _, _]] {
    override def apply[A: Arbitrary]: URIO[RndEnv, A] = ZIO.accessM(_.get.apply[A])
  }

  type LadderEnv   = Has[Ladder[IO]]
  type ProfilesEnv = Has[Profiles[IO]]
  type RanksEnv    = Has[Ranks[IO]]
  type RndEnv      = Has[Rnd[IO]]

}
