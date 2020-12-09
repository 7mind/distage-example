package leaderboard

import com.typesafe.config.ConfigFactory
import distage.{DIKey, Injector}
import izumi.distage.config.AppConfigModule
import izumi.distage.model.definition.Activation
import izumi.distage.model.definition.StandardAxis.Repo
import izumi.distage.model.plan.Roots
import izumi.distage.testkit.scalatest.Spec2
import izumi.logstage.api.logger.LogRouter
import leaderboard.axis.Scene
import leaderboard.plugins.{LeaderboardPlugin, PostgresDockerPlugin, ZIOPlugin}
import logstage.di.LogstageModule
import zio.{IO, Task}

final class WiringTest extends Spec2[IO] {
  "all dependencies are wired correctly" in {
    def checkActivation(activation: Activation): Task[Unit] = {
      Injector()
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
          activation,
          Roots(DIKey[LeaderboardRole[IO]]),
        )
        .assertValid[Task]()
    }

    for {
      _ <- checkActivation(Activation(Repo -> Repo.Dummy))
      _ <- checkActivation(Activation(Repo -> Repo.Prod, Scene -> Scene.Provided))
      _ <- checkActivation(Activation(Repo -> Repo.Prod, Scene -> Scene.Managed))
    } yield ()
  }
}
