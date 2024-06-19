package leaderboard.http

import cats.effect.Async
import cats.implicits.*
import com.comcast.ip4s.Port
import fs2.io.net.Network
import izumi.distage.model.definition.Lifecycle
import leaderboard.api.HttpApi
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

final case class HttpServer(
  server: Server
)

object HttpServer {

  final class Impl[F[_]](
    allHttpApis: Set[HttpApi[F]]
  )(implicit
    async: Async[F]
  ) extends Lifecycle.Of[F, HttpServer](
      Lifecycle.fromCats {
        val combinedApis = allHttpApis.map(_.http).toList.foldK

        EmberServerBuilder
          .default(async, Network.forAsync)
          .withHttpApp(combinedApis.orNotFound)
          .withPort(Port.fromInt(8080).get)
          .build
          .map(HttpServer(_))
      }
    )

}
