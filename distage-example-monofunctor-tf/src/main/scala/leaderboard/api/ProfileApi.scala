package leaderboard.api

import cats.effect.Concurrent
import io.circe.syntax.*
import cats.implicits.*
import leaderboard.model.UserProfile
import leaderboard.repo.Profiles
import leaderboard.services.Ranks
import logstage.LogIO
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

final class ProfileApi[F[_]: Concurrent](
  dsl: Http4sDsl[F],
  profiles: Profiles[F],
  ranks: Ranks[F],
  log: LogIO[F],
) extends HttpApi[F] {

  import dsl.*

  override def http: HttpRoutes[F] = {
    HttpRoutes.of {
      case GET -> Root / "profile" / UUIDVar(userId) =>
        Ok(for {
          res <- ranks.getRank(userId)
        } yield res.asJson)

      case rq @ POST -> Root / "profile" / UUIDVar(userId) =>
        Ok(for {
          profile <- rq.decodeJson[UserProfile]
          _       <- log.info(s"Saving $profile")
          _       <- profiles.setProfile(userId, profile)
        } yield ())
    }
  }
}
