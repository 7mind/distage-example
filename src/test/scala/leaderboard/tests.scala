package leaderboard

import com.typesafe.config.ConfigFactory
import distage.{DIKey, Injector, ModuleDef}
import leaderboard.model.{QueryFailure, Score, UserId, UserProfile}
import leaderboard.plugins.{LeaderboardPlugin, ZIOPlugin}
import leaderboard.repo.{Ladder, Profiles}
import leaderboard.zioenv._
import izumi.distage.config.AppConfigModule
import izumi.distage.framework.model.PluginSource
import izumi.distage.model.definition.Activation
import izumi.distage.model.definition.StandardAxis.Repo
import izumi.distage.model.plan.GCMode
import izumi.distage.plugins.load.PluginLoader.PluginConfig
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.scalatest.DistageBIOSpecScalatest
import izumi.distage.testkit.services.DISyntaxZIOEnv
import izumi.logstage.api.logger.LogRouter
import logstage.di.LogstageModule
import org.scalatest.WordSpec
import zio.{IO, Task, ZIO}

abstract class leaderboardTest extends DistageBIOSpecScalatest[IO] with DISyntaxZIOEnv {
  override def config = TestConfig(
    pluginSource = Some(PluginSource(PluginConfig(packagesEnabled = Seq("leaderboard.plugins")))),
    activation   = Activation(Repo -> Repo.Prod),
    moduleOverrides = new ModuleDef {
      make[Rnd[IO]].from[Rnd.Impl[IO]]
      include(PostgresDockerModule)
    },
    memoizationRoots = Set(
      DIKey.get[Ladder[IO]],
      DIKey.get[Profiles[IO]],
    ),
  )
}

trait DummyTest extends leaderboardTest {
  override final def config = super.config.copy(
    activation = Activation(Repo -> Repo.Dummy),
  )
}

final class LadderTestDummy extends LadderTest with DummyTest
final class ProfilesTestDummy extends ProfilesTest with DummyTest
final class RanksTestDummy extends RanksTest with DummyTest

final class LadderTestPostgres extends LadderTest
final class ProfilesTestPostgres extends ProfilesTest
final class RanksTestPostgres extends RanksTest

abstract class LadderTest extends leaderboardTest {

  "Ladder" should {
    // this test gets dependencies through arguments
    "submit & get" in {
      (rnd: Rnd[IO], ladder: Ladder[IO]) =>
        for {
          user  <- rnd[UserId]
          score <- rnd[Score]
          _     <- ladder.submitScore(user, score)
          res   <- ladder.getScores.map(_.find(_._1 == user).map(_._2))
          _     = assert(res contains score)
        } yield ()
    }

    // other tests get dependencies via ZIO Env:
    "assign a higher position in the list to a higher score" in {
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

        _ = if (score1 > score2) {
          assert(user1Rank < user2Rank)
        } else if (score2 > score1) {
          assert(user2Rank < user1Rank)
        }
      } yield ()
    }
  }

}

abstract class ProfilesTest extends leaderboardTest {
  "Profiles" should {
    // that's what the env signature looks like for ZIO Env injection
    "set & get" in {
      val zioValue: ZIO[ProfilesEnv with RndEnv, QueryFailure, Unit] = for {
        user    <- rnd[UserId]
        name    <- rnd[String]
        desc    <- rnd[String]
        profile = UserProfile(name, desc)
        _       <- profiles.setProfile(user, profile)
        res     <- profiles.getProfile(user)
        _       = assert(res contains profile)
      } yield ()
      zioValue
    }
  }
}

abstract class RanksTest extends leaderboardTest {
  "Ranks" should {
    "return None for a user with no score" in {
      for {
        user    <- rnd[UserId]
        name    <- rnd[String]
        desc    <- rnd[String]
        profile = UserProfile(name, desc)
        _       <- profiles.setProfile(user, profile)
        res1    <- ranks.getRank(user)
        _       = assert(res1.isEmpty)
      } yield ()
    }

    "return None for a user with no profile" in {
      for {
        user  <- rnd[UserId]
        score <- rnd[Score]
        _     <- ladder.submitScore(user, score)
        res1  <- ranks.getRank(user)
        _     = assert(res1.isEmpty)
      } yield ()
    }

    "assign a higher rank to a user with more score" in {
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

        _ = if (score1 > score2) {
          assert(user1Rank < user2Rank)
        } else if (score2 > score1) {
          assert(user2Rank < user1Rank)
        }
      } yield ()
    }
  }
}

final class InjectionTest extends leaderboardTest with DummyTest {
  "all dependencies are wired" in {
    () =>
      def checkActivation(activation: Activation): Task[Unit] = {
        val plan = Injector(activation).plan(
          input = Seq(
            LeaderboardPlugin,
            ZIOPlugin,
            // dummy logger + config modules,
            // normally the RoleStarter or the testkit will provide real values here
            new LogstageModule(LogRouter.nullRouter, false),
            new AppConfigModule(ConfigFactory.empty),
          ).merge,
          gcMode = GCMode(DIKey.get[LeaderboardRole[zio.IO]])
        )
        Task(plan.assertImportsResolvedOrThrow())
      }

      checkActivation(Activation(Repo -> Repo.Dummy)) *>
      checkActivation(Activation(Repo -> Repo.Prod))
  }
}
