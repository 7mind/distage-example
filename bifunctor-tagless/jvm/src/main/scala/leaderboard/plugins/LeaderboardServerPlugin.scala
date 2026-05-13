package leaderboard.plugins

import distage.StandardAxis.Repo
import distage.config.ConfigModuleDef
import distage.{ModuleDef, Scene, TagKK}
import doobie.util.transactor.Transactor
import izumi.distage.plugins.PluginDef
import izumi.fundamentals.platform.integration.PortCheck
import leaderboard.config.{PostgresCfg, PostgresPortCfg}
import leaderboard.http.HttpServer
import leaderboard.repo.{Ladder, LadderPostgres, Profiles, ProfilesPostgres}
import leaderboard.sql.{SQL, TransactorResource}
import zio.IO

import scala.concurrent.duration.*

/**
  * JVM-only `PluginDef` adding the http4s server, the postgres
  * repositories, and the related configs. Companion to
  * [[LeaderboardCorePlugin]] (which carries the roles, APIs, and the
  * in-memory dummy repos shared with the Scala.js simulation).
  */
object LeaderboardServerPlugin extends PluginDef {
  include(modules.server[IO])
  include(modules.repoProd[IO])
  include(modules.configs)
  include(modules.prodConfigs)

  object modules {
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
