package leaderboard.http

import cats.effect.Async
import cats.syntax.all.*
import com.comcast.ip4s.Port
import fs2.io.net.Network
import izumi.distage.model.definition.Lifecycle
import leaderboard.api.HttpApi
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Location
import org.http4s.server.Server
import org.http4s.server.middleware.CORS
import org.http4s.server.staticcontent.resourceServiceBuilder
import org.http4s.{HttpRoutes, Uri}

final case class HttpServer(
  server: Server
)

object HttpServer {

  final class Impl[F[+_, +_]](
    allHttpApis: Set[HttpApi[F]],
    dsl: Http4sDsl[F[Throwable, _]],
  )(implicit
    async: Async[F[Throwable, _]]
  ) extends Lifecycle.Of[F[Throwable, _], HttpServer](
      Lifecycle.fromCats {
        type G[A] = F[Throwable, A]
        import dsl.*

        // The same HttpRoutes the LocalDispatcher invokes in-process.
        val apiRoutes: HttpRoutes[G] = allHttpApis.map(_.http).toList.foldK

        // Static demo UI in classpath under /webapp. The resource builder maps
        // request paths verbatim (e.g. GET /main.js -> classpath /webapp/main.js),
        // so a tiny extra route redirects GET / to /index.html.
        val indexRedirect: HttpRoutes[G] = HttpRoutes.of[G] {
          case GET -> Root => PermanentRedirect(Location(Uri.unsafeFromString("/index.html")))
        }
        val staticRoutes: HttpRoutes[G] = resourceServiceBuilder[G]("/webapp").toRoutes

        // Permissive CORS so the same UI also works when opened from file://
        // (its origin is "null" in that case). Demo-grade — do not copy
        // verbatim into a real production service.
        val app = CORS.policy.withAllowOriginAll(
          (apiRoutes <+> indexRedirect <+> staticRoutes).orNotFound
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
