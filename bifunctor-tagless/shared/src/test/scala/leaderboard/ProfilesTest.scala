package leaderboard

import leaderboard.model.*
import leaderboard.repo.Profiles
import leaderboard.zioenv.*
import zio.{IO, ZIO}

abstract class ProfilesTest extends LeaderboardTestBase {

  "Profiles" should {

    /** that's what the ZIO signature looks like for ZIO Env injection: */
    "set & get" in {
      val zioValue: ZIO[Profiles[IO] & Rnd[IO], QueryFailure, Unit] = for {
        user   <- rnd[UserId]
        name   <- rnd[String]
        desc   <- rnd[String]
        profile = UserProfile(name, desc)
        _      <- profiles.setProfile(user, profile)
        res    <- profiles.getProfile(user)
        _      <- assertIO(res contains profile)
      } yield ()
      zioValue
    }

  }

}

final class ProfilesTestDummy extends ProfilesTest with DummyTest
