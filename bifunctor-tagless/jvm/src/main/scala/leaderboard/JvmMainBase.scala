package leaderboard

import distage.Activation
import distage.plugins.PluginConfig
import izumi.fundamentals.platform.IzPlatform
import izumi.fundamentals.platform.cli.model.RoleArgs
import leaderboard.plugins.{LeaderboardCorePlugin, LeaderboardServerPlugin, PostgresDockerPlugin}

/**
  * JVM-only [[MainBase]] specialisation. Overrides `pluginConfig` to
  * include the server/postgres/docker plugins alongside the shared
  * [[LeaderboardCorePlugin]]: `PluginConfig.cached` discovery during
  * development, explicit `PluginConfig.const` under GraalVM Native
  * Image (where classpath scanning is unreliable).
  */
abstract class JvmMainBase(
  activation: Activation,
  requiredRolesList: Vector[RoleArgs],
) extends MainBase(activation, requiredRolesList) {

  override def pluginConfig: PluginConfig = {
    if (IzPlatform.isGraalNativeImage) {
      // Only this would work reliably for NativeImage
      PluginConfig.const(List(LeaderboardCorePlugin, LeaderboardServerPlugin, PostgresDockerPlugin))
    } else {
      // Runtime discovery with PluginConfig.cached might be convenient for pure jvm projects during active development
      // Once the project gets to the maintenance stage it's a good idea to switch to PluginConfig.const
      PluginConfig.cached(pluginsPackage = "leaderboard.plugins")
    }
  }

}
