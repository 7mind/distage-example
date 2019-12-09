package livecode

import distage.{DIKey, ModuleDef}
import doobie.util.transactor.Transactor
import izumi.distage.docker.examples.PostgresDocker
import izumi.distage.framework.model.PluginSource
import izumi.distage.model.definition.Activation
import izumi.distage.model.definition.StandardAxis.Repo
import izumi.distage.plugins.load.PluginLoader.PluginConfig
import izumi.distage.testkit.TestConfig
import izumi.distage.testkit.scalatest.DistageBIOSpecScalatest
import izumi.distage.testkit.services.DISyntaxZIOEnv
import livecode.code._
import livecode.zioenv._
import zio.{IO, Task, ZIO}

abstract class LivecodeTest extends DistageBIOSpecScalatest[IO] with DISyntaxZIOEnv {
  override def config = TestConfig(
    pluginSource = Some(PluginSource(PluginConfig(packagesEnabled = Seq("livecode.plugins")))),
    activation   = Activation(Repo -> Repo.Prod),
    moduleOverrides = new ModuleDef {
      make[Rnd[IO]].from[Rnd.Impl[IO]]
      include(PostgresDockerModule)
    },
    memoizedKeys = Set(
      DIKey.get[Transactor[Task]],
      DIKey.get[Ladder[IO]],
      DIKey.get[Profiles[IO]],
      DIKey.get[PostgresDocker.Container],
    ),
  )
}

trait DummyTest extends LivecodeTest {
  override final def config = super.config.copy(
    activation = Activation(Repo -> Repo.Dummy),
  )
}

final class LadderTestDummy extends LadderTestPostgres with DummyTest
final class ProfilesTestDummy extends ProfilesTestPostgres with DummyTest
final class RanksTestDummy extends RanksTestPostgres with DummyTest

class LadderTestPostgres extends LivecodeTest with DummyTest {

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
    "return higher score higher in the list" in {
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

class ProfilesTestPostgres extends LivecodeTest {
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

class RanksTestPostgres extends LivecodeTest {
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
