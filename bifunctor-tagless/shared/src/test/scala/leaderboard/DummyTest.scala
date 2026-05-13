package leaderboard

import izumi.distage.model.definition.Activation
import izumi.distage.model.definition.StandardAxis.Repo

trait DummyTest extends LeaderboardTestBase {
  override final def config = super.config.copy(
    activation = super.config.activation ++ Activation(Repo -> Repo.Dummy)
  )
}
