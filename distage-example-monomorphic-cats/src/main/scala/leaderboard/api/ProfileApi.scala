package leaderboard.api

import cats.effect.IO
import io.circe.syntax.*
import leaderboard.model.UserProfile
import leaderboard.repo.Profiles
import leaderboard.services.Ranks
import logstage.LogIO
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

final class ProfileApi(
  dsl: Http4sDsl[IO],
  profiles: Profiles,
  ranks: Ranks,
  log: LogIO[IO],
) extends HttpApi {

  import dsl.*

  override def http: HttpRoutes[IO] = {
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
