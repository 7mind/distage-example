package leaderboard

import cats.effect.IO
import distage.{DIKey, ModuleDef, Scene}
import izumi.distage.model.definition.Activation
import izumi.distage.model.definition.StandardAxis.Repo
import izumi.distage.plugins.PluginConfig
import izumi.distage.testkit.scalatest.{AssertCIO, Spec1}
import leaderboard.model.*
import leaderboard.repo.{Ladder, Profiles}
import leaderboard.services.Ranks

abstract class LeaderboardTest extends Spec1[IO] with AssertCIO {
  override def config = super.config.copy(
    pluginConfig = PluginConfig.cached(packagesEnabled = Seq("leaderboard.plugins")),
    moduleOverrides = super.config.moduleOverrides ++ new ModuleDef {
      make[Rnd].from[Rnd.Impl]
    },
    // For testing, setup a docker container with postgres,
    // instead of trying to connect to an external database
    activation = Activation(Scene -> Scene.Managed),
    // Instantiate Ladder & Profiles only once per test-run and
    // share them and all their dependencies across all tests.
    // this includes the Postgres Docker container above and table DDLs
    memoizationRoots = Set(
      DIKey[Ladder],
      DIKey[Profiles],
    ),
  )
}

trait DummyTest extends LeaderboardTest {
  override final def config = super.config.copy(
    activation = super.config.activation ++ Activation(Repo -> Repo.Dummy)
  )
}

trait ProdTest extends LeaderboardTest {
  override final def config = super.config.copy(
    activation = super.config.activation ++ Activation(Repo -> Repo.Prod)
  )
}

final class LadderTestDummy extends LadderTest with DummyTest
final class ProfilesTestDummy extends ProfilesTest with DummyTest
final class RanksTestDummy extends RanksTest with DummyTest

final class LadderTestPostgres extends LadderTest with ProdTest
final class ProfilesTestPostgres extends ProfilesTest with ProdTest
final class RanksTestPostgres extends RanksTest with ProdTest

abstract class LadderTest extends LeaderboardTest {

  "Ladder" should {

    /** this test gets dependencies injected through function arguments */
    "submit & get" in {
      (rnd: Rnd, ladder: Ladder) =>
        for {
          user   <- rnd[UserId]
          score  <- rnd[Score]
          _      <- ladder.submitScore(user, score)
          scores <- ladder.getScores
          res     = scores.find(_._1 == user).map(_._2)
          _      <- assertIO(res contains score)
        } yield ()
    }

    "assign a higher position in the list to a higher score" in {
      (rnd: Rnd, ladder: Ladder) =>
        for {
          user1  <- rnd[UserId]
          score1 <- rnd[Score]
          user2  <- rnd[UserId]
          score2 <- rnd[Score]

          _      <- ladder.submitScore(user1, score1)
          _      <- ladder.submitScore(user2, score2)
          scores <- ladder.getScores

          user1Rank = scores.indexWhere(_._1 == user1)
          user2Rank = scores.indexWhere(_._1 == user2)

          _ <-
            if (score1 > score2) {
              assertIO(user1Rank < user2Rank)
            } else if (score2 > score1) {
              assertIO(user2Rank < user1Rank)
            } else IO.unit
        } yield ()
    }

  }

}

abstract class ProfilesTest extends LeaderboardTest {

  "Profiles" should {

    "set & get" in {
      (rnd: Rnd, profiles: Profiles) =>
        for {
          user   <- rnd[UserId]
          name   <- rnd[String]
          desc   <- rnd[String]
          profile = UserProfile(name, desc)
          _      <- profiles.setProfile(user, profile)
          res    <- profiles.getProfile(user)
          _      <- assertIO(res contains profile)
        } yield ()
    }

  }

}

abstract class RanksTest extends LeaderboardTest {

  "Ranks" should {

    "return 0 rank for a user with no score" in {
      (rnd: Rnd, ranks: Ranks, profiles: Profiles) =>
        for {
          user   <- rnd[UserId]
          name   <- rnd[String]
          desc   <- rnd[String]
          profile = UserProfile(name, desc)
          _      <- profiles.setProfile(user, profile)
          res1   <- ranks.getRank(user)
          _      <- assertIO(res1.contains(RankedProfile(name, desc, 0, 0)))
        } yield ()
    }

    "return None for a user with no profile" in {
      (rnd: Rnd, ranks: Ranks, ladder: Ladder) =>
        for {
          user  <- rnd[UserId]
          score <- rnd[Score]
          _     <- ladder.submitScore(user, score)
          res1  <- ranks.getRank(user)
          _     <- assertIO(res1.isEmpty)
        } yield ()
    }

    "assign a higher rank to a user with more score" in {
      (rnd: Rnd, ranks: Ranks, ladder: Ladder, profiles: Profiles) =>
        for {
          user1  <- rnd[UserId]
          name1  <- rnd[String]
          desc1  <- rnd[String]
          score1 <- rnd[Score]

          user2  <- rnd[UserId]
          name2  <- rnd[String]
          desc2  <- rnd[String]
          score2 <- rnd[Score]

          _ <- profiles.setProfile(user1, UserProfile(name1, desc1))
          _ <- ladder.submitScore(user1, score1)

          _ <- profiles.setProfile(user2, UserProfile(name2, desc2))
          _ <- ladder.submitScore(user2, score2)

          user1Rank <- ranks.getRank(user1).map(_.get.rank)
          user2Rank <- ranks.getRank(user2).map(_.get.rank)

          _ <-
            if (score1 > score2) {
              assertIO(user1Rank < user2Rank)
            } else if (score2 > score1) {
              assertIO(user2Rank < user1Rank)
            } else IO.unit
        } yield ()
    }

  }

}
