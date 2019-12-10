package example.plugins

import distage.TagKK
import distage.plugins.PluginDef
import doobie.util.transactor.Transactor
import example.ExampleRole
import example.config.{PostgresCfg, PostgresPortCfg}
import example.http.HttpApi
import example.repo.{Ladder, Profiles, Ranks}
import example.sql.{SQL, TransactorResource}
import izumi.distage.config.ConfigModuleDef
import izumi.distage.model.definition.ModuleDef
import izumi.distage.model.definition.StandardAxis.Repo
import izumi.fundamentals.platform.integration.PortCheck
import org.http4s.dsl.Http4sDsl
import zio.IO

object ExamplePlugin extends PluginDef {
  include(modules.api[IO])
  include(modules.repoDummy[IO])
  include(modules.repoProd[IO])
  include(modules.configProd)

  object modules {
    def api[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
      make[ExampleRole[F]]

      make[HttpApi[F]].from[HttpApi.Impl[F]]
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
      make[PortCheck].from(new PortCheck(3))
    }

    val configProd = new ConfigModuleDef {
      makeConfig[PostgresCfg]("postgres")
      makeConfig[PostgresPortCfg]("postgres")
    }
  }
}
