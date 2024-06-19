package leaderboard.api

import cats.effect.IO
import org.http4s.HttpRoutes

trait HttpApi {
  def http: HttpRoutes[IO]
}
