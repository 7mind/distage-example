package leaderboard

import com.typesafe.config.ConfigFactory
import distage.{DIKey, Injector}
import izumi.distage.config.AppConfigModule
import izumi.distage.model.definition.Activation
import izumi.distage.model.definition.StandardAxis.Repo
import izumi.distage.model.plan.Roots
import izumi.distage.testkit.scalatest.DistageBIOSpecScalatest
import izumi.logstage.api.logger.LogRouter
import leaderboard.axis.Services
import leaderboard.plugins.{LeaderboardPlugin, PostgresDockerPlugin, ZIOPlugin}
import logstage.di.LogstageModule
import zio.{IO, Task}

final class WiringTest extends DistageBIOSpecScalatest[IO] {
  "all dependencies are wired correctly" in {
    def checkActivation(activation: Activation): Task[Unit] = {
      Task {
        Injector(activation)
          .plan(
            Seq(
              LeaderboardPlugin,
              ZIOPlugin,
              PostgresDockerPlugin,
              // dummy logger + config modules,
              // normally the RoleAppMain or the testkit will provide real values here
              new LogstageModule(LogRouter.nullRouter, setupStaticLogRouter = false),
              new AppConfigModule(ConfigFactory.empty),
            ).merge,
            Roots(DIKey[LeaderboardRole[IO]]),
          )
          .assertImportsResolvedOrThrow()
      }
    }

    for {
      _ <- checkActivation(Activation(Repo -> Repo.Dummy))
      _ <- checkActivation(Activation(Repo -> Repo.Prod, Services -> Services.Prod))
      _ <- checkActivation(Activation(Repo -> Repo.Prod, Services -> Services.Docker))
    } yield ()
  }
}
