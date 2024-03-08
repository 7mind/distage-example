package leaderboard

import distage.StandardAxis.Repo
import distage.plugins.PluginConfig
import distage.{Activation, Lifecycle, Module, ModuleDef}
import izumi.distage.model.definition.StandardAxis.Scene
import izumi.distage.roles.RoleAppMain
import izumi.distage.roles.bundled.{ConfigWriter, Help}
import izumi.distage.roles.model.{RoleDescriptor, RoleService}
import izumi.functional.bio.Applicative2
import izumi.fundamentals.platform.IzPlatform
import izumi.fundamentals.platform.cli.model.raw.{RawEntrypointParams, RawRoleParams, RawValue}
import leaderboard.api.{LadderApi, ProfileApi}
import leaderboard.http.HttpServer
import leaderboard.plugins.{LeaderboardPlugin, PostgresDockerPlugin}
import logstage.LogIO2
import zio.IO

import scala.annotation.unused

/**
  * A role that exposes just the /ladder/ endpoints, it can be launched with
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
final class LadderRole[F[+_, +_]: Applicative2](
  @unused ladderApi: LadderApi[F],
  @unused runningServer: HttpServer,
  log: LogIO2[F],
) extends RoleService[F[Throwable, _]] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): Lifecycle[F[Throwable, _], Unit] = {
    Lifecycle.liftF(log.info("Ladder API started!"))
  }
}
object LadderRole extends RoleDescriptor {
  final val id = "ladder"
}

/**
  * A role that exposes just the /profile/ endpoints, it can be launched with
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
final class ProfileRole[F[+_, +_]: Applicative2](
  @unused profileApi: ProfileApi[F],
  @unused runningServer: HttpServer,
  log: LogIO2[F],
) extends RoleService[F[Throwable, _]] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): Lifecycle[F[Throwable, _], Unit] = {
    Lifecycle.liftF(log.info("Profile API started!"))
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
  *   # check leaderboard
  *   curl -X GET http://localhost:8080/ladder
  *   # user profile now shows the rank in the ladder along with profile data
  *   curl -X GET http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4
  * }}}
  */
final class LeaderboardRole[F[+_, +_]: Applicative2](
  @unused ladderRole: LadderRole[F],
  @unused profileRole: ProfileRole[F],
  log: LogIO2[F],
) extends RoleService[F[Throwable, _]] {
  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): Lifecycle[F[Throwable, _], Unit] = {
    Lifecycle.liftF(log.info("Ladder & Profile APIs started!"))
  }
}
object LeaderboardRole extends RoleDescriptor {
  final val id = "leaderboard"
}

/**
  * Launch the service with dummy configuration.
  *
  * This will use in-memory repositories and not require an external postgres DB.
  *
  * Equivalent to:
  * {{{
  *   ./launcher -u repo:dummy :leaderboard
  * }}}
  */
object MainDummy extends MainBase(Activation(Repo -> Repo.Dummy), Vector(RawRoleParams(LeaderboardRole.id)))

/**
  * Launch with production configuration and setup the required postgres DB inside docker.
  *
  * You will need docker daemon running in the background.
  *
  * Equivalent to:
  * {{{
  *   ./launcher -u scene:managed :leaderboard
  * }}}
  */
object MainProdDocker extends MainBase(Activation(Repo -> Repo.Prod, Scene -> Scene.Managed), Vector(RawRoleParams(LeaderboardRole.id)))

/**
  * Launch with production configuration and external, not dockerized, services.
  *
  * You will need postgres to be available at `localhost:5432`.
  * To set it up with Docker, execute the following command:
  *
  * {{{
  *   docker run --rm -d -p 5432:5432 postgres:12.1
  * }}}
  *
  * Equivalent to:
  * {{{
  *   ./launcher :leaderboard
  * }}}
  */
object MainProd extends MainBase(Activation(Repo -> Repo.Prod, Scene -> Scene.Provided), Vector(RawRoleParams(LeaderboardRole.id)))

/**
  * Launch just the `ladder` APIs with dummy repositories
  *
  * Equivalent to:
  * {{{
  *   ./launcher -u repo:dummy :ladder
  * }}}
  */
object MainLadderDummy extends MainBase(Activation(Repo -> Repo.Dummy), Vector(RawRoleParams(LadderRole.id)))

/**
  * Launch just the `ladder` APIs with postgres repositories and dockerized postgres service
  *
  * Equivalent to:
  * {{{
  *   ./launcher -u scene:managed :ladder
  * }}}
  */
object MainLadderProdDocker extends MainBase(Activation(Repo -> Repo.Prod, Scene -> Scene.Managed), Vector(RawRoleParams(LadderRole.id)))

/**
  * Launch just the `ladder` APIs with postgres repositories and external postgres service
  *
  * You will need postgres to be available at `localhost:5432`
  *
  * Equivalent to:
  * {{{
  *   ./launcher :ladder
  * }}}
  */
object MainLadderProd extends MainBase(Activation(Repo -> Repo.Prod, Scene -> Scene.Provided), Vector(RawRoleParams(LadderRole.id)))

/**
  * Launch just the `profile` APIs with dummy repositories
  *
  * Equivalent to:
  * {{{
  *   ./launcher -u repo:dummy :profile
  * }}}
  */
object MainProfileDummy extends MainBase(Activation(Repo -> Repo.Dummy), Vector(RawRoleParams(ProfileRole.id)))

/**
  * Launch just the `ladder` APIs with postgres repositories and dockerized postgres service
  *
  * Equivalent to:∂
  * {{{
  *   ./launcher -u scene:managed :profile
  * }}}
  */
object MainProfileProdDocker extends MainBase(Activation(Repo -> Repo.Prod, Scene -> Scene.Managed), Vector(RawRoleParams(ProfileRole.id)))

/**
  * Launch just the `profile` APIs with postgres repositories and external postgres service
  *
  * Equivalent to:
  * {{{
  *   ./launcher :profile
  * }}}
  */
object MainProfileProd extends MainBase(Activation(Repo -> Repo.Prod, Scene -> Scene.Provided), Vector(RawRoleParams(ProfileRole.id)))

/**
  * Display help message with all available launcher arguments
  * and command-line parameters for all roles
  *
  * Equivalent to:
  * {{{
  *   ./launcher :help
  * }}}
  */
object MainHelp extends MainBase(Activation(Repo -> Repo.Prod, Scene -> Scene.Provided), Vector(RawRoleParams(Help.id)))

/**
  * Write the default configuration files for each role into JSON files in `./config`.
  * Configurations in @see {{{izumi.distage.config.ConfigModuleDef#makeConfig}}}
  * are read from resources:
  *
  *   - common-reference.conf - (configuration shared across all roles)
  *   - \${roleName}-reference.conf - (role-specific configuration, overrides `common`)
  *
  * Equivalent to:
  * {{{
  *   ./launcher :configwriter
  * }}}
  */
object MainWriteReferenceConfigs
  extends MainBase(
    activation = {
      Activation(Repo -> Repo.Prod, Scene -> Scene.Provided)
    },
    requiredRoles = {
      Vector(
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
    },
  )

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
object GenericLauncher extends MainBase(Activation(Repo -> Repo.Prod, Scene -> Scene.Provided), Vector.empty)

sealed abstract class MainBase(
  activation: Activation,
  requiredRoles: Vector[RawRoleParams],
) extends RoleAppMain.LauncherBIO[IO] {

  override def requiredRoles(argv: RoleAppMain.ArgV): Vector[RawRoleParams] = {
    requiredRoles
  }

  override def pluginConfig: PluginConfig = {
    if (IzPlatform.isGraalNativeImage) {
      // Only this would work reliably for NativeImage
      PluginConfig.const(List(LeaderboardPlugin, PostgresDockerPlugin))
    } else {
      // Runtime discovery with PluginConfig.cached might be convenient for pure jvm projects during active development
      // Once the project gets to the maintenance stage it's a good idea to switch to PluginConfig.const
      PluginConfig.cached(pluginsPackage = "leaderboard.plugins")
    }
  }

  protected override def roleAppBootOverrides(argv: RoleAppMain.ArgV): Module = super.roleAppBootOverrides(argv) ++ new ModuleDef {
    make[Activation].named("default").fromValue(defaultActivation ++ activation)
  }

  private def defaultActivation = Activation(Scene -> Scene.Provided)

}
