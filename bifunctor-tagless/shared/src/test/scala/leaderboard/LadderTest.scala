package leaderboard

import leaderboard.model.*
import leaderboard.repo.Ladder
import leaderboard.zioenv.*
import zio.{IO, ZIO}

abstract class LadderTest extends LeaderboardTestBase {

  "Ladder" should {

    /** this test gets dependencies injected through function arguments */
    "submit & get" in {
      (rnd: Rnd[IO], ladder: Ladder[IO]) =>
        for {
          user  <- rnd[UserId]
          score <- rnd[Score]
          _     <- ladder.submitScore(user, score)
          res   <- ladder.getScores.map(_.find(_._1 == user).map(_._2))
          _     <- assertIO(res contains score)
        } yield ()
    }

    /** this test get dependencies injected via ZIO Env: */
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

        _ <-
          if (score1 > score2) {
            assertIO(user1Rank < user2Rank)
          } else if (score2 > score1) {
            assertIO(user2Rank < user1Rank)
          } else ZIO.unit
      } yield ()
    }

  }

}

final class LadderTestDummy extends LadderTest with DummyTest
