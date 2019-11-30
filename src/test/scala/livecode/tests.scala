package livecode

import distage.{DIKey, ModuleDef}
import doobie.util.transactor.Transactor
import izumi.distage.constructors.ConcreteConstructor
import izumi.distage.model.definition.StandardAxis
import izumi.distage.testkit.integration.docker.examples.{PostgresDocker, PostgresDockerModule}
import izumi.distage.testkit.services.DISyntaxZIOEnv
import izumi.distage.testkit.services.st.dtest.TestConfig
import izumi.distage.testkit.st.specs.DistageBIOSpecScalatest
import livecode.code.Postgres.PgIntegrationCheck
import livecode.code._
import livecode.zioenv._
import zio.{IO, Task, ZIO}

abstract class LivecodeTest extends DistageBIOSpecScalatest[IO] with DISyntaxZIOEnv {
  override def config = TestConfig(
    pluginPackages = Some(Seq("livecode.plugins")),
    activation     = StandardAxis.testProdActivation,
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
    activation = StandardAxis.testDummyActivation,
  )
}

final class LadderTestDummy extends LadderTest with DummyTest
final class ProfilesTestDummy extends ProfilesTest with DummyTest
final class RanksTestDummy extends RanksTest with DummyTest

class LadderTest extends LivecodeTest with DummyTest {

  "Ladder" should {
    "submit & get" in {
//      (rnd: Rnd[IO], ladder: Ladder[IO]) =>
      for {
        user  <- rnd[UserId]
        score <- rnd[Score]
        _     <- ladder.submitScore(user, score)
        res   <- ladder.getScores.map(_.find(_._1 == user).map(_._2))
        _     = assert(res contains score)
      } yield ()
    }

    "return higher score higher in the list" in {
//      (rnd: Rnd[IO], ladder: Ladder[IO]) =>
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

class ProfilesTest extends LivecodeTest {
  "Profiles" should {
    "set & get" in {
//      (rnd: Rnd[IO], profiles: Profiles[IO]) =>
      for {
        user    <- rnd[UserId]
        name    <- rnd[String]
        desc    <- rnd[String]
        profile = UserProfile(name, desc)
        _       <- profiles.setProfile(user, profile)
        res     <- profiles.getProfile(user)
        _       = assert(res contains profile)
      } yield ()
    }
  }
}

class RanksTest extends LivecodeTest {
  "Ranks" should {
    "return None for a user with no score" in {
//      (rnd: Rnd[IO], ranks: Ranks[IO], profiles: Profiles[IO]) =>
      val value: ZIO[RanksEnv with ProfilesEnv with RndEnv, QueryFailure, Unit] = for {
        user    <- rnd[UserId]
        name    <- rnd[String]
        desc    <- rnd[String]
        profile = UserProfile(name, desc)
        _       <- profiles.setProfile(user, profile)
        res1    <- ranks.getRank(user)
        _       = assert(res1.isEmpty)
      } yield ()
      value
    }

    "return None for a user with no profile" in {
//      (rnd: Rnd[IO], ranks: Ranks[IO], ladder: Ladder[IO]) =>
      for {
        user  <- rnd[UserId]
        score <- rnd[Score]
        _     <- ladder.submitScore(user, score)
        res1  <- ranks.getRank(user)
        _     = assert(res1.isEmpty)
      } yield ()
    }

    "assign a higher rank to a user with more score" in {
//      (rnd: Rnd[IO], ranks: Ranks[IO], ladder: Ladder[IO], profiles: Profiles[IO]) =>
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
