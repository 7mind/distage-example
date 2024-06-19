package leaderboard.api

import org.http4s.HttpRoutes

trait HttpApi[F[_]] {
  def http: HttpRoutes[F]
}
