package leaderboard.plugins

import distage.StandardAxis.Repo
import distage.config.ConfigModuleDef
import distage.{ModuleDef, Scene, TagKK}
import org.typelevel.doobie.util.transactor.Transactor
import izumi.distage.plugins.PluginDef
import izumi.distage.roles.bundled.BundledRolesModule
import izumi.distage.roles.model.definition.RoleModuleDef
import izumi.fundamentals.platform.integration.PortCheck
import izumi.fundamentals.platform.versions.Version
import leaderboard.config.{PostgresCfg, PostgresPortCfg}
import leaderboard.http.HttpServer
import leaderboard.repo.{Ladder, LadderPostgres, Profiles, ProfilesPostgres}
import leaderboard.sql.{SQL, TransactorResource}
import leaderboard.{LadderRole, LeaderboardRole, ProfileRole}
import zio.IO

import scala.concurrent.duration.*

object LeaderboardPlugin extends PluginDef {
  include(modules.roles[IO])
  // Shared API + dummy repos wiring — same code that runs in the browser.
  include(LeaderboardCoreModule.api[IO])
  include(LeaderboardCoreModule.repoDummy[IO])
  include(modules.repoProd[IO])
  include(modules.server[IO])
  include(modules.configs)
  include(modules.prodConfigs)

  object modules {
    def roles[F[+_, +_]: TagKK]: RoleModuleDef = new RoleModuleDef {
      // The `ladder` role
      makeRole[LadderRole[F]]

      // The `profile` role
      makeRole[ProfileRole[F]]

      // The composite `leaderboard` role that pulls in both `ladder` & `profile` roles
      makeRole[LeaderboardRole[F]]

      // Add bundled roles: `help` & `configwriter`
      include(BundledRolesModule[F[Throwable, _]](version = Version.parse("1.0.0")))
    }

    def server[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
      make[HttpServer].fromResource[HttpServer.Impl[F]]
    }

    def repoProd[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
      tag(Repo.Prod)

      make[Ladder[F]].fromResource[LadderPostgres[F]]
      make[Profiles[F]].fromResource[ProfilesPostgres[F]]

      make[SQL[F]].from[SQL.Impl[F]]

      make[Transactor[F[Throwable, _]]].fromResource[TransactorResource[F[Throwable, _]]]
      make[PortCheck].from(new PortCheck(3.seconds))
    }

    val configs: ConfigModuleDef = new ConfigModuleDef {
      makeConfig[PostgresCfg]("postgres")
    }
    val prodConfigs: ConfigModuleDef = new ConfigModuleDef {
      // only use this if Scene axis is set to Provided
      tag(Scene.Provided)

      makeConfig[PostgresPortCfg]("postgres")
    }
  }
}
