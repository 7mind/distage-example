package leaderboard

import distage.{DIKey, ModuleDef}
import izumi.distage.plugins.PluginConfig
import izumi.distage.testkit.scalatest.{AssertZIO, SpecZIO}
import leaderboard.plugins.LeaderboardCorePlugin
import leaderboard.repo.{Ladder, Profiles}
import zio.IO

abstract class LeaderboardTestBase extends SpecZIO with AssertZIO {
  override def config = super.config.copy(
    pluginConfig    = PluginConfig.const(List(LeaderboardCorePlugin)),
    moduleOverrides = super.config.moduleOverrides ++ new ModuleDef {
      make[Rnd[IO]].from[Rnd.Impl[IO]]
    },
    memoizationRoots = Set(
      DIKey[Ladder[IO]],
      DIKey[Profiles[IO]],
    ),
  )
}
