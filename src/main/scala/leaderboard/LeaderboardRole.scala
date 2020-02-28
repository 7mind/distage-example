package leaderboard

import cats.effect.{ConcurrentEffect, Timer}
import distage.StandardAxis.Repo
import distage.plugins.PluginConfig
import distage.{Activation, DIResource, DIResourceBase}
import izumi.distage.roles.bundled.{ConfigWriter, Help}
import izumi.distage.roles.model.{RoleDescriptor, RoleService}
import izumi.distage.roles.{RoleAppLauncher, RoleAppMain}
import izumi.fundamentals.platform.cli.model.raw.{RawEntrypointParams, RawRoleParams, RawValue}
import leaderboard.http.HttpApi
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

/** Example session:
  *
  * {{{
  * curl -X POST http://localhost:8080/ladder/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4/100
  * curl -X POST http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4 -d '{"name": "Kai", "description": "S C A L A"}'
  * curl -X GET http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4
  * curl -X GET http://localhost:8080/ladder
  * }}}
  */
final class LeaderboardRole[F[+_, +_]](
  httpApi: HttpApi[F],
)(implicit
  concurrentEffect: ConcurrentEffect[F[Throwable, ?]],
  timer: Timer[F[Throwable, ?]],
) extends RoleService[F[Throwable, ?]] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): DIResourceBase[F[Throwable, ?], Unit] = {
    DIResource.fromCats {
      BlazeServerBuilder[F[Throwable, ?]]
        .withHttpApp(httpApi.http.orNotFound)
        .bindLocal(8080)
        .resource
    }.void
  }
}

object LeaderboardRole extends RoleDescriptor {
  val id = "leaderboard"
}

object MainDummy extends MainBase(Activation(Repo -> Repo.Dummy))

/**
  * To launch production configuration, you need postgres to be available at `localhost:5432`.
  * To set it up with Docker, execute the following command:
  *
  * {{{
  *   docker run -d -p 5432:5432 postgres:12.1
  * }}}
  */
object MainProd extends MainBase(Activation(Repo -> Repo.Prod))

/**
  * Display help message with all available launcher arguments
  * and command-line parameters for all roles
  */
object MainHelp extends MainBase(Activation(Repo -> Repo.Prod)) {
  override val requiredRoles = Vector(
    RawRoleParams(Help.id)
  )
}

/**
  * Write the default configuration files for each role into JSON files in `./config`.
  * Configurations in [[izumi.distage.config.ConfigModuleDef#makeConfig]]
  * are read from resources:
  *
  *   - common-reference.conf - (configuration shared across all roles)
  *   - ${roleName}-reference.conf - (role-specific configuration, overrides `common`)
  *
  */
object MainWriteReferenceConfigs extends MainBase(Activation(Repo -> Repo.Prod)) {
  override val requiredRoles = Vector(
    RawRoleParams(
      role = ConfigWriter.id,
      roleParameters = RawEntrypointParams(
        flags = Vector.empty,
        // output configs in "hocon" format, instead of "json"
        values = Vector(RawValue("format", "hocon")),
      ),
      freeArgs = Vector.empty,
    )
  )
}

/**
  * Generic launcher not set to run a specific role by default,
  * use command-line arguments to choose one or multiple roles:
  *
  * {{{
  * # launch app with prod repositories
  *
  *   ./launcher :leaderboard
  *
  * # launch app with dummy repositories
  *
  *   ./launcher -u repo:dummy :leaderboard
  *
  * # display help
  *
  *   ./launcher :help
  *
  * # write configs in HOCON format to ./default-configs
  *
  *   ./launcher :configwriter -format hocon -t default-configs
  *
  * # print help, dump configs and launch app with dummy repositories
  *
  *   ./launcher -u repo:dummy :help :configwriter :leaderboard
  * }}}
  */
object GenericLauncher extends MainBase(Activation(Repo -> Repo.Prod)) {
  override val requiredRoles = Vector.empty
}

sealed abstract class MainBase(activation: Activation)
  extends RoleAppMain.Default(
    launcher = new RoleAppLauncher.LauncherBIO[zio.IO] {
      override val pluginConfig        = PluginConfig.cached(packagesEnabled = Seq("leaderboard.plugins"))
      override val requiredActivations = activation
    }
  ) {
  override val requiredRoles = Vector(
    RawRoleParams(LeaderboardRole.id)
  )
}
