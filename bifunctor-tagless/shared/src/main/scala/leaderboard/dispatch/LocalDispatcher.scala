package leaderboard.dispatch

import cats.effect.Async
import cats.syntax.all.*
import leaderboard.api.HttpApi
import leaderboard.dispatch.LocalDispatcher.Response
import org.http4s.{HttpApp, Method, Request, Uri}

import scala.annotation.unused

/**
  * Invokes the same `HttpRoutes` that the http4s server exposes, but without
  * a network hop. Used by the http4s server on the JVM as the actual handler
  * for incoming requests, and by the `@JSExportTopLevel` entrypoint in the
  * browser so the front-end can call the simulated backend exactly like it
  * would call the real one.
  */
trait LocalDispatcher[F[_, _]] {
  def call(method: String, path: String, body: String): F[Throwable, Response]
}

object LocalDispatcher {
  final case class Response(status: Int, body: String)

  final class Impl[F[+_, +_]](
    @unused apis: Set[HttpApi[F]]
  )(implicit
    async: Async[F[Throwable, _]]
  ) extends LocalDispatcher[F] {
    private type G[A] = F[Throwable, A]

    // Pre-assembled handler matching what the http4s server runs.
    private val httpApp: HttpApp[G] = apis.map(_.http).toList.foldK.orNotFound

    override def call(method: String, path: String, body: String): F[Throwable, Response] = {
      val req = Request[G](
        method = Method.fromString(method).getOrElse(Method.GET),
        uri    = Uri.unsafeFromString(path),
      ).withEntity(body)
      httpApp.run(req).flatMap { resp =>
        resp.as[String].map(text => Response(resp.status.code, text))
      }
    }
  }
}
