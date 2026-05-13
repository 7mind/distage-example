package leaderboard

import izumi.distage.model.definition.Activation
import izumi.distage.model.definition.StandardAxis.{Repo, Scene}
import izumi.distage.plugins.PluginConfig

trait ProdTest extends LeaderboardTestBase {
  override final def config = super.config.copy(
    pluginConfig = PluginConfig.cached(packagesEnabled = Seq("leaderboard.plugins")),
    activation   = super.config.activation ++ Activation(Repo -> Repo.Prod, Scene -> Scene.Managed),
  )
}
