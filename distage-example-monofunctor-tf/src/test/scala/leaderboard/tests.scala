package leaderboard

import cats.Applicative
import cats.effect.Sync
import cats.syntax.all.*
import distage.{DIKey, DefaultModule, ModuleDef, Scene}
import izumi.distage.model.definition.Activation
import izumi.distage.model.definition.StandardAxis.Repo
import izumi.distage.testkit.scalatest.{AssertSync, Spec1}
import izumi.distage.testkit.spec.TestConfiguration
import izumi.reflect.TagK
import leaderboard.model.*
import leaderboard.repo.{Ladder, Profiles}
import leaderboard.services.Ranks
import zio.interop.catz.asyncInstance

abstract class LeaderboardTest[F[_]: TagK: DefaultModule] extends Spec1[F] with AssertSync[F] {
  override def config = super.config.copy(
    moduleOverrides = super.config.moduleOverrides ++ new ModuleDef {
      make[Rnd[F]].from[Rnd.Impl[F]]
    },
    // For testing, set up a docker container with postgres,
    // instead of trying to connect to an external database
    activation = Activation(Scene -> Scene.Managed),
    // Instantiate Ladder & Profiles only once per test-run and
    // share them and all their dependencies across all tests.
    // this includes the Postgres Docker container above and table DDLs
    memoizationRoots = Set(
      DIKey[Ladder[F]],
      DIKey[Profiles[F]],
    ),
  )
}

trait DummyTest extends TestConfiguration {
  abstract override def config = super.config.copy(
    activation = super.config.activation ++ Activation(Repo -> Repo.Dummy)
  )
}

trait ProdTest extends TestConfiguration {
  abstract override def config = super.config.copy(
    activation = super.config.activation ++ Activation(Repo -> Repo.Prod)
  )
}

trait CatsTest extends LeaderboardTest[cats.effect.IO] {
  abstract override def config = super.config.copy(
    // For memoization across test suites to work predictably, PluginDef
    // instances used in pluginConfig should be the same in all the test suites.
    // We use pluginConfig from a global val to ensure that instances are shared.
    pluginConfig = GenericLauncherCats.pluginConfig
  )
}

trait ZIOTest extends LeaderboardTest[zio.Task] {
  abstract override def config = super.config.copy(
    pluginConfig = GenericLauncherZIO.pluginConfig
  )
}

final class LadderTestDummyCats extends LadderTest[cats.effect.IO] with DummyTest with CatsTest
final class ProfilesTestDummyCats extends ProfilesTest[cats.effect.IO] with DummyTest with CatsTest
final class RanksTestDummyCats extends RanksTest[cats.effect.IO] with DummyTest with CatsTest

final class LadderTestPostgresCats extends LadderTest[cats.effect.IO] with ProdTest with CatsTest
final class ProfilesTestPostgresCats extends ProfilesTest[cats.effect.IO] with ProdTest with CatsTest
final class RanksTestPostgresCats extends RanksTest[cats.effect.IO] with ProdTest with CatsTest

final class LadderTestDummyZIO extends LadderTest[zio.Task] with DummyTest with ZIOTest
final class ProfilesTestDummyZIO extends ProfilesTest[zio.Task] with DummyTest with ZIOTest
final class RanksTestDummyZIO extends RanksTest[zio.Task] with DummyTest with ZIOTest

final class LadderTestPostgresZIO extends LadderTest[zio.Task] with ProdTest with ZIOTest
final class ProfilesTestPostgresZIO extends ProfilesTest[zio.Task] with ProdTest with ZIOTest
final class RanksTestPostgresZIO extends RanksTest[zio.Task] with ProdTest with ZIOTest

abstract class LadderTest[F[_]: Sync: TagK: DefaultModule] extends LeaderboardTest[F] {

  "Ladder" should {

    /** this test gets dependencies injected through function arguments */
    "submit & get" in {
      (rnd: Rnd[F], ladder: Ladder[F]) =>
        for {
          user  <- rnd[UserId]
          score <- rnd[Score]
          _     <- ladder.submitScore(user, score)
          res   <- ladder.getScores.map(_.find(_._1 == user).map(_._2))
          _     <- assertIO(res contains score)
        } yield ()
    }

    "assign a higher position in the list to a higher score" in {
      (rnd: Rnd[F], ladder: Ladder[F]) =>
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
            } else Applicative[F].unit
        } yield ()
    }

  }

}

abstract class ProfilesTest[F[_]: Sync: TagK: DefaultModule] extends LeaderboardTest[F] {

  "Profiles" should {

    "set & get" in {
      (rnd: Rnd[F], profiles: Profiles[F]) =>
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

abstract class RanksTest[F[_]: Sync: TagK: DefaultModule] extends LeaderboardTest[F] {

  "Ranks" should {

    "return 0 rank for a user with no score" in {
      (rnd: Rnd[F], ranks: Ranks[F], profiles: Profiles[F]) =>
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
      (rnd: Rnd[F], ranks: Ranks[F], ladder: Ladder[F]) =>
        for {
          user  <- rnd[UserId]
          score <- rnd[Score]
          _     <- ladder.submitScore(user, score)
          res1  <- ranks.getRank(user)
          _     <- assertIO(res1.isEmpty)
        } yield ()
    }

    "assign a higher rank to a user with more score" in {
      (rnd: Rnd[F], ranks: Ranks[F], ladder: Ladder[F], profiles: Profiles[F]) =>
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
            } else Applicative[F].unit
        } yield ()
    }

  }

}
