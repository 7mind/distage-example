package leaderboard.plugins

import cats.effect.IO
import distage.StandardAxis.Repo
import distage.config.ConfigModuleDef
import distage.plugins.PluginDef
import distage.{ModuleDef, Scene}
import doobie.util.transactor.Transactor
import izumi.distage.roles.bundled.BundledRolesModule
import izumi.distage.roles.model.definition.RoleModuleDef
import izumi.fundamentals.platform.integration.PortCheck
import leaderboard.api.{HttpApi, LadderApi, ProfileApi}
import leaderboard.config.{PostgresCfg, PostgresPortCfg}
import leaderboard.http.HttpServer
import leaderboard.repo.{Ladder, Profiles}
import leaderboard.services.Ranks
import leaderboard.sql.{SQL, TransactorResource}
import leaderboard.{LadderRole, LeaderboardRole, ProfileRole}
import org.http4s.dsl.Http4sDsl

import scala.concurrent.duration.*

object LeaderboardPlugin extends PluginDef {
  include(modules.roles)
  include(modules.api)
  include(modules.repoDummy)
  include(modules.repoProd)
  include(modules.configs)
  include(modules.prodConfigs)

  object modules {
    def roles: RoleModuleDef = new RoleModuleDef {
      // The `ladder` role
      makeRole[LadderRole]

      // The `profile` role
      makeRole[ProfileRole]

      // The composite `leaderboard` role that pulls in both `ladder` & `profile` roles
      makeRole[LeaderboardRole]

      // Add bundled roles: `help` & `configwriter`
      include(BundledRolesModule[IO](version = "1.0.0"))
    }

    def api: ModuleDef = new ModuleDef {
      // The `ladder` API
      make[LadderApi]
      // The `profile` API
      make[ProfileApi]

      // A set of all APIs
      many[HttpApi]
        .weak[LadderApi] // add ladder API as a _weak reference_
        .weak[ProfileApi] // add profiles API as a _weak reference_

      make[HttpServer].fromResource[HttpServer.Impl]

      make[Ranks].from[Ranks.Impl]

      makeTrait[Http4sDsl[IO]]
    }

    def repoDummy: ModuleDef = new ModuleDef {
      tag(Repo.Dummy)

      make[Ladder].fromResource[Ladder.Dummy]
      make[Profiles].fromResource[Profiles.Dummy]
    }

    def repoProd: ModuleDef = new ModuleDef {
      tag(Repo.Prod)

      make[Ladder].fromResource[Ladder.Postgres]
      make[Profiles].fromResource[Profiles.Postgres]

      make[SQL].from[SQL.Impl]

      make[Transactor[IO]].fromResource[TransactorResource]
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
