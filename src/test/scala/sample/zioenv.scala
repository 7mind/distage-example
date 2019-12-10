package sample

import sample.model._
import sample.repo.{Ladder, Profiles, Ranks}
import org.scalacheck.Arbitrary
import zio.{IO, URIO, ZIO}

object zioenv {

  object ladder extends Ladder[ZIO[LadderEnv, ?, ?]] {
    def submitScore(userId: UserId, score: Score): ZIO[LadderEnv, QueryFailure, Unit] = ZIO.accessM(_.ladder.submitScore(userId, score))
    def getScores: ZIO[LadderEnv, QueryFailure, List[(UserId, Score)]]                = ZIO.accessM(_.ladder.getScores)
  }

  object profiles extends Profiles[ZIO[ProfilesEnv, ?, ?]] {
    override def setProfile(userId: UserId, profile: UserProfile): ZIO[ProfilesEnv, QueryFailure, Unit] = ZIO.accessM(_.profiles.setProfile(userId, profile))
    override def getProfile(userId: UserId): ZIO[ProfilesEnv, QueryFailure, Option[UserProfile]]        = ZIO.accessM(_.profiles.getProfile(userId))
  }

  object ranks extends Ranks[ZIO[RanksEnv, ?, ?]] {
    override def getRank(userId: UserId): ZIO[RanksEnv, QueryFailure, Option[RankedProfile]] = ZIO.accessM(_.ranks.getRank(userId))
  }

  object rnd extends Rnd[ZIO[RndEnv, ?, ?]] {
    override def apply[A: Arbitrary]: URIO[RndEnv, A] = ZIO.accessM(_.rnd.apply[A])
  }

  trait LadderEnv {
    def ladder: Ladder[IO]
  }

  trait ProfilesEnv {
    def profiles: Profiles[IO]
  }

  trait RanksEnv {
    def ranks: Ranks[IO]
  }

  trait RndEnv {
    def rnd: Rnd[IO]
  }

}
