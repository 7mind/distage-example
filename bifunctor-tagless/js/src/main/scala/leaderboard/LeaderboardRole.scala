package leaderboard

import distage.Lifecycle
import izumi.distage.roles.model.{RoleDescriptor, RoleService}
import izumi.functional.bio.Applicative2
import izumi.fundamentals.platform.cli.model.EntrypointArgs
import leaderboard.api.{LadderApi, ProfileApi}
import logstage.LogIO2

import scala.annotation.unused

/**
  * JS-side role classes. Same class names and role IDs as the JVM
  * variants in `jvm/src/main/scala/leaderboard/LeaderboardRole.scala`,
  * but without the `HttpServer` constructor dep — the in-browser
  * simulation surfaces routes via the in-process `LocalDispatcher`
  * rather than an http4s server.
  */

final class LadderRole[F[+_, +_]: Applicative2](
  @unused ladderApi: LadderApi[F],
  log: LogIO2[F],
) extends RoleService[F[Throwable, _]] {
  override def start(roleParameters: EntrypointArgs): Lifecycle[F[Throwable, _], Unit] = {
    Lifecycle.liftF(log.info("Ladder API started!"))
  }
}
object LadderRole extends RoleDescriptor {
  final val id = "ladder"
}

final class ProfileRole[F[+_, +_]: Applicative2](
  @unused profileApi: ProfileApi[F],
  log: LogIO2[F],
) extends RoleService[F[Throwable, _]] {
  override def start(roleParameters: EntrypointArgs): Lifecycle[F[Throwable, _], Unit] = {
    Lifecycle.liftF(log.info("Profile API started!"))
  }
}
object ProfileRole extends RoleDescriptor {
  final val id = "profile"
}

final class LeaderboardRole[F[+_, +_]: Applicative2](
  @unused ladderRole: LadderRole[F],
  @unused profileRole: ProfileRole[F],
  log: LogIO2[F],
) extends RoleService[F[Throwable, _]] {
  override def start(roleParameters: EntrypointArgs): Lifecycle[F[Throwable, _], Unit] = {
    Lifecycle.liftF(log.info("Ladder & Profile APIs started!"))
  }
}
object LeaderboardRole extends RoleDescriptor {
  final val id = "leaderboard"
}
