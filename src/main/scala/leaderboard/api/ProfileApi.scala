package leaderboard.api

import io.circe.syntax.*
import izumi.functional.bio.catz.*
import izumi.functional.bio.{Async2, Fork2, Primitives2}
import leaderboard.model.UserProfile
import leaderboard.repo.Profiles
import leaderboard.services.Ranks
import logstage.LogIO2
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl

final class ProfileApi[F[+_, +_]: Async2: Fork2: Primitives2](
  dsl: Http4sDsl[F[Throwable, _]],
  profiles: Profiles[F],
  ranks: Ranks[F],
  log: LogIO2[F],
) extends HttpApi[F] {

  import dsl.*

  override def http: HttpRoutes[F[Throwable, _]] = {
    HttpRoutes.of {
      case GET -> Root / "profile" / UUIDVar(userId) =>
        Ok(ranks.getRank(userId).map(_.asJson))

      case rq @ POST -> Root / "profile" / UUIDVar(userId) =>
        Ok(for {
          profile <- rq.decodeJson[UserProfile]
          _       <- log.info(s"Saving $profile")
          _       <- profiles.setProfile(userId, profile)
        } yield ())
    }
  }
}
