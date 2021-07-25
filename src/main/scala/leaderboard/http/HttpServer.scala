package leaderboard.http

import cats.effect.{ConcurrentEffect, Timer}
import cats.implicits._
import distage.Id
import izumi.distage.model.definition.Lifecycle
import leaderboard.api.HttpApi
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Server
import org.http4s.syntax.kleisli._

import scala.concurrent.ExecutionContext

final case class HttpServer(
  server: Server
)

object HttpServer {

  final class Impl[F[+_, +_]](
    allHttpApis: Set[HttpApi[F]],
    cpuPool: ExecutionContext @Id("zio.cpu"),
  )(implicit
    concurrentEffect: ConcurrentEffect[F[Throwable, _]],
    timer: Timer[F[Throwable, _]],
  ) extends Lifecycle.Of[F[Throwable, _], HttpServer](
      Lifecycle.fromCats {
        val combinedApis = allHttpApis.map(_.http).toList.foldK

        BlazeServerBuilder[F[Throwable, _]](cpuPool)
          .withHttpApp(combinedApis.orNotFound)
          .bindLocal(8080)
          .resource
          .map(HttpServer(_))
      }
    )

}
