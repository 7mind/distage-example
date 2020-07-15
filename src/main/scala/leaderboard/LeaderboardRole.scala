package leaderboard

import distage.StandardAxis.Repo
import distage.plugins.PluginConfig
import distage.{Activation, DIResource, DIResourceBase}
import izumi.distage.roles.bundled.{ConfigWriter, Help}
import izumi.distage.roles.model.{RoleDescriptor, RoleService}
import izumi.distage.roles.{RoleAppLauncher, RoleAppMain}
import izumi.functional.bio.BIOApplicative
import izumi.fundamentals.platform.cli.model.raw.{RawEntrypointParams, RawRoleParams, RawValue}
import leaderboard.api.{LadderApi, ProfileApi}
import leaderboard.http.HttpServer
import logstage.LogBIO

import scala.annotation.unused

/** A role that exposes just the /ladder/ endpoints, it can be launched with
  *
  * {{{
  *   ./launcher :ladder
  * }}}
  *
  * Example session:
  *
  * {{{
  *   curl -X POST http://localhost:8080/ladder/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4/100
  *   curl -X GET http://localhost:8080/ladder
  * }}}
  */
final class LadderRole[F[+_, +_]: BIOApplicative](
  @unused ladderApi: LadderApi[F],
  @unused runningServer: HttpServer[F],
  log: LogBIO[F],
) extends RoleService[F[Throwable, ?]] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): DIResourceBase[F[Throwable, ?], Unit] = {
    DIResource.liftF(log.info("Ladder API started!"))
  }
}
object LadderRole extends RoleDescriptor {
  final val id = "ladder"
}

/** A role that exposes just the /profile/ endpoints, it can be launched with
  *
  * {{{
  *   ./launcher :profile
  * }}}
  *
  * Example session:
  *
  * {{{
  *   curl -X POST http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4 -d '{"name": "Kai", "description": "S C A L A"}'
  *   curl -X GET http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4
  * }}}
  */
final class ProfileRole[F[+_, +_]: BIOApplicative](
  @unused profileApi: ProfileApi[F],
  @unused runningServer: HttpServer[F],
  log: LogBIO[F],
) extends RoleService[F[Throwable, ?]] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): DIResourceBase[F[Throwable, ?], Unit] = {
    DIResource.liftF(log.info("Profile API started!"))
  }
}
object ProfileRole extends RoleDescriptor {
  final val id = "profile"
}

/** A composite role that exposes all the endpoints, for convenience, it can be launched with
  *
  * {{{
  *   ./launcher :leaderboard
  * }}}
  *
  * Note that this will have the same effect as launching both [[LadderRole]] and [[ProfileRole]] at the same time.
  *
  * {{{
  *   ./launcher :ladder :profile
  * }}}
  *
  * Example session:
  *
  * {{{
  *   curl -X POST http://localhost:8080/ladder/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4/100
  *   curl -X POST http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4 -d '{"name": "Kai", "description": "S C A L A"}'
  *   # user profile now shows the rank in the ladder along with profile data
  *   curl -X GET http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4
  * }}}
  */
final class LeaderboardRole[F[+_, +_]: BIOApplicative](
  @unused ladderRole: LadderRole[F],
  @unused profileRole: ProfileRole[F],
  log: LogBIO[F],
) extends RoleService[F[Throwable, ?]] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): DIResourceBase[F[Throwable, ?], Unit] = {
    DIResource.liftF(log.info("Ladder & Profile APIs started!"))
  }
}
object LeaderboardRole extends RoleDescriptor {
  final val id = "leaderboard"
}

/**
  * Launch the service with dummy configuration.
  * This will use in-memory repositories and not require an external postgres DB.
  */
object MainDummy extends MainBase(Activation(Repo -> Repo.Dummy))

/**
  * To launch production configuration, you need postgres to be available at `localhost:5432`.
  * To set it up with Docker, execute the following command:
  *
  * {{{
  *   docker run --rm -d -p 5432:5432 postgres:12.1
  * }}}
  */
object MainProd extends MainBase(Activation(Repo -> Repo.Prod))

/** Launch just the `ladder` APIs with dummy repositories */
object MainLadderDummy extends MainBase(Activation(Repo -> Repo.Dummy), Vector(RawRoleParams(LadderRole.id)))

/** Launch just the `ladder` APIs with postgres repositories */
object MainLadderProd extends MainBase(Activation(Repo -> Repo.Prod), Vector(RawRoleParams(LadderRole.id)))

/** Launch just the `profile` APIs with dummy repositories */
object MainProfileDummy extends MainBase(Activation(Repo -> Repo.Dummy), Vector(RawRoleParams(ProfileRole.id)))

/** Launch just the `profile` APIs with postgres repositories */
object MainProfileProd extends MainBase(Activation(Repo -> Repo.Prod), Vector(RawRoleParams(ProfileRole.id)))

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
  *
  *   # launch app with prod repositories
  *
  *   ./launcher :leaderboard
  *
  *   # launch app with dummy repositories
  *
  *   ./launcher -u repo:dummy :leaderboard
  *
  *   # launch just the ladder API, without profiles
  *
  *   ./launcher :ladder
  *
  *   # display help
  *
  *   ./launcher :help
  *
  *   # write configs in HOCON format to ./default-configs
  *
  *   ./launcher :configwriter -format hocon -t default-configs
  *
  *   # print help, dump configs and launch app with dummy repositories
  *
  *   ./launcher -u repo:dummy :help :configwriter :leaderboard
  *
  * }}}
  */
object GenericLauncher extends MainBase(Activation(Repo -> Repo.Prod)) {
  override val requiredRoles = Vector.empty
}

sealed abstract class MainBase(
  activation: Activation,
  override val requiredRoles: Vector[RawRoleParams] = Vector(RawRoleParams(LeaderboardRole.id)),
) extends RoleAppMain.Default(
    launcher = new RoleAppLauncher.LauncherBIO[zio.IO] {
      override val pluginConfig        = PluginConfig.cached(packagesEnabled = Seq("leaderboard.plugins"))
      override val requiredActivations = activation
    }
  )
