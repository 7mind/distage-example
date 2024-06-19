package leaderboard.api

import cats.effect.IO
import io.circe.syntax.*
import leaderboard.repo.Ladder
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

final class LadderApi(
  dsl: Http4sDsl[IO],
  ladder: Ladder,
) extends HttpApi {

  import dsl.*

  override def http: HttpRoutes[IO] = {
    HttpRoutes.of {
      case GET -> Root / "ladder" =>
        Ok(for {
          res <- ladder.getScores
        } yield res.asJson)

      case POST -> Root / "ladder" / UUIDVar(userId) / LongVar(score) =>
        Ok(for {
          _ <- ladder.submitScore(userId, score)
        } yield ())
    }
  }
}
