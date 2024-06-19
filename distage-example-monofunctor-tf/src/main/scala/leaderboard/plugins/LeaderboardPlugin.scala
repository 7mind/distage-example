package leaderboard.plugins

import distage.StandardAxis.Repo
import distage.config.ConfigModuleDef
import distage.plugins.PluginDef
import distage.{ModuleDef, Scene, TagK}
import doobie.util.transactor.Transactor
import izumi.distage.roles.bundled.BundledRolesModule
import izumi.distage.roles.model.definition.RoleModuleDef
import izumi.fundamentals.platform.integration.PortCheck
import zio.Task
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
  include(modules.roles[Task])
  include(modules.api[Task])
  include(modules.repoDummy[Task])
  include(modules.repoProd[Task])
  include(modules.configs)
  include(modules.prodConfigs)

  object modules {
    def roles[F[_]: TagK]: RoleModuleDef = new RoleModuleDef {
      // The `ladder` role
      makeRole[LadderRole[F]]

      // The `profile` role
      makeRole[ProfileRole[F]]

      // The composite `leaderboard` role that pulls in both `ladder` & `profile` roles
      makeRole[LeaderboardRole[F]]

      // Add bundled roles: `help` & `configwriter`
      include(BundledRolesModule[F](version = "1.0.0"))
    }

    def api[F[_]: TagK]: ModuleDef = new ModuleDef {
      // The `ladder` API
      make[LadderApi[F]]
      // The `profile` API
      make[ProfileApi[F]]

      // A set of all APIs
      many[HttpApi[F]]
        .weak[LadderApi[F]] // add ladder API as a _weak reference_
        .weak[ProfileApi[F]] // add profiles API as a _weak reference_

      make[HttpServer].fromResource[HttpServer.Impl[F]]

      make[Ranks[F]].from[Ranks.Impl[F]]

      makeTrait[Http4sDsl[F]]
    }

    def repoDummy[F[_]: TagK]: ModuleDef = new ModuleDef {
      tag(Repo.Dummy)

      make[Ladder[F]].fromResource[Ladder.Dummy[F]]
      make[Profiles[F]].fromResource[Profiles.Dummy[F]]
    }

    def repoProd[F[_]: TagK]: ModuleDef = new ModuleDef {
      tag(Repo.Prod)

      make[Ladder[F]].fromResource[Ladder.Postgres[F]]
      make[Profiles[F]].fromResource[Profiles.Postgres[F]]

      make[SQL[F]].from[SQL.Impl[F]]

      make[Transactor[F]].fromResource[TransactorResource[F]]
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
