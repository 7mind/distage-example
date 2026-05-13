package leaderboard.http

import cats.effect.Async
import cats.syntax.all.*
import com.comcast.ip4s.Port
import fs2.io.net.Network
import izumi.distage.model.definition.Lifecycle
import leaderboard.api.HttpApi
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.CORS
import org.http4s.server.staticcontent.resourceServiceBuilder

final case class HttpServer(
  server: Server
)

object HttpServer {

  final class Impl[F[+_, +_]](
    allHttpApis: Set[HttpApi[F]]
  )(implicit
    async: Async[F[Throwable, _]]
  ) extends Lifecycle.Of[F[Throwable, _], HttpServer](
      Lifecycle.fromCats {
        type G[A] = F[Throwable, A]

        // The same HttpRoutes the LocalDispatcher invokes in-process.
        val apiRoutes: HttpRoutes[G] = allHttpApis.map(_.http).toList.foldK

        // Serve /webapp resources at the root, so the demo UI is available
        // at e.g. http://localhost:8080/ alongside the API endpoints.
        val staticRoutes: HttpRoutes[G] =
          resourceServiceBuilder[G]("/webapp").toRoutes

        // Permissive CORS so the same UI also works when opened from file://
        // (its origin is "null" in that case). Demo-grade — do not copy
        // verbatim into a real production service.
        val app = CORS.policy.withAllowOriginAll(
          (apiRoutes <+> staticRoutes).orNotFound
        )

        EmberServerBuilder
          .default(using async, Network.forAsync)
          .withHttpApp(app)
          .withPort(Port.fromInt(8080).get)
          .build
          .map(HttpServer(_))
      }
    )

}
