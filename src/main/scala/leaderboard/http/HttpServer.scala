package leaderboard.http

import cats.effect.{ConcurrentEffect, Timer}
import cats.implicits._
import distage.Id
import izumi.distage.model.definition.Lifecycle
import leaderboard.api.HttpApi
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

import scala.concurrent.ExecutionContext

final case class HttpServer[F[_, _]](
  server: Server[F[Throwable, ?]]
)

object HttpServer {

  final class Impl[F[+_, +_]](
    allHttpApis: Set[HttpApi[F]],
    cpuPool: ExecutionContext @Id("zio.cpu"),
  )(implicit
    concurrentEffect: ConcurrentEffect[F[Throwable, ?]],
    timer: Timer[F[Throwable, ?]],
  ) extends Lifecycle.Of[F[Throwable, ?], HttpServer[F]](
      Lifecycle.fromCats {
        val combinedApis = allHttpApis.map(_.http).toList.foldK

        BlazeServerBuilder[F[Throwable, ?]](cpuPool)
          .withHttpApp(combinedApis.orNotFound)
          .bindLocal(8080)
          .resource
          .map(HttpServer(_))
      }
    )

}
