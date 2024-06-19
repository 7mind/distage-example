package leaderboard.http

import cats.effect.IO
import cats.implicits.*
import com.comcast.ip4s.Port
import izumi.distage.model.definition.Lifecycle
import leaderboard.api.HttpApi
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

final case class HttpServer(
  server: Server
)

object HttpServer {

  final class Impl(
    allHttpApis: Set[HttpApi]
  ) extends Lifecycle.Of[IO, HttpServer](
      Lifecycle.fromCats {
        val combinedApis = allHttpApis.map(_.http).toList.foldK

        EmberServerBuilder
          .default[IO]
          .withHttpApp(combinedApis.orNotFound)
          .withPort(Port.fromInt(8080).get)
          .build
          .map(HttpServer(_))
      }
    )

}
