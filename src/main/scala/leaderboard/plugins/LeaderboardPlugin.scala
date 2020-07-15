package leaderboard.plugins

import distage.StandardAxis.Repo
import distage.config.ConfigModuleDef
import distage.plugins.PluginDef
import distage.{ModuleDef, TagKK}
import doobie.util.transactor.Transactor
import izumi.distage.roles.bundled.BundledRolesModule
import izumi.fundamentals.platform.integration.PortCheck
import leaderboard.{LadderRole, LeaderboardRole, ProfileRole}
import leaderboard.api.{HttpApi, LadderApi, ProfileApi}
import leaderboard.config.{PostgresCfg, PostgresPortCfg}
import leaderboard.http.HttpServer
import leaderboard.repo.{Ladder, Profiles, Ranks}
import leaderboard.sql.{SQL, TransactorResource}
import org.http4s.dsl.Http4sDsl
import zio.IO

import scala.concurrent.duration._

object LeaderboardPlugin extends PluginDef {
  include(modules.roles[IO])
  include(modules.api[IO])
  include(modules.repoDummy[IO])
  include(modules.repoProd[IO])
  include(modules.configs)

  object modules {
    def roles[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
      // The `ladder` role
      make[LadderRole[F]]

      // The `profile` role
      make[ProfileRole[F]]

      // The composite `leaderboard` role that pulls in both `ladder` & `profile` roles
      make[LeaderboardRole[F]]

      // Add bundled roles: `help` & `configwriter`
      include(BundledRolesModule[F[Throwable, ?]](version = "1.0.0-SNAPSHOT"))
    }

    def api[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
      // The `ladder` API
      make[LadderApi[F]]
      // The `profile` API
      make[ProfileApi[F]]

      // A set of all APIs
      many[HttpApi[F]]
        .weak[LadderApi[F]] // add ladder API as a _weak reference_
        .weak[ProfileApi[F]] // add profiles API as a _weak reference_

      make[HttpServer[F]].fromResource[HttpServer.Impl[F]]

      make[Ranks[F]].from[Ranks.Impl[F]]

      make[Http4sDsl[F[Throwable, ?]]]
    }

    def repoDummy[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
      tag(Repo.Dummy)

      make[Ladder[F]].fromResource[Ladder.Dummy[F]]
      make[Profiles[F]].fromResource[Profiles.Dummy[F]]
    }

    def repoProd[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
      tag(Repo.Prod)

      make[Ladder[F]].fromResource[Ladder.Postgres[F]]
      make[Profiles[F]].fromResource[Profiles.Postgres[F]]

      make[SQL[F]].from[SQL.Impl[F]]

      make[Transactor[F[Throwable, ?]]].fromResource[TransactorResource[F[Throwable, ?]]]
      make[PortCheck].from(new PortCheck(3.seconds))
    }

    val configs: ConfigModuleDef = new ConfigModuleDef {
      makeConfig[PostgresCfg]("postgres")
      makeConfig[PostgresPortCfg]("postgres")
    }
  }
}
