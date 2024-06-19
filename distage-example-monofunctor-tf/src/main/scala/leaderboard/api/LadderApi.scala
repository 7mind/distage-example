package leaderboard.api

import cats.MonadThrow
import cats.implicits.*
import io.circe.syntax.*
import leaderboard.repo.Ladder
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

final class LadderApi[F[_]: MonadThrow](
  dsl: Http4sDsl[F],
  ladder: Ladder[F],
) extends HttpApi[F] {

  import dsl.*

  override def http: HttpRoutes[F] = {
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
