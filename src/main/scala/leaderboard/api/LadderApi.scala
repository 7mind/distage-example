package leaderboard.api

import io.circe.syntax.*
import izumi.functional.bio.IO2
import izumi.functional.bio.catz.*
import leaderboard.repo.Ladder
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

final class LadderApi[F[+_, +_]: IO2](
  dsl: Http4sDsl[F[Throwable, _]],
  ladder: Ladder[F],
) extends HttpApi[F] {

  import dsl.*

  override def http: HttpRoutes[F[Throwable, _]] = {
    HttpRoutes.of {
      case GET -> Root / "ladder" =>
        Ok(ladder.getScores.map(_.asJson))

      case POST -> Root / "ladder" / UUIDVar(userId) / LongVar(score) =>
        Ok(ladder.submitScore(userId, score))
    }
  }
}
