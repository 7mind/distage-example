package leaderboard

import distage.plugins.PluginConfig
import distage.{Activation, Module, ModuleDef}
import izumi.distage.model.definition.StandardAxis.Scene
import izumi.distage.roles.RoleAppMain
import izumi.fundamentals.platform.cli.model.RoleArgs
import leaderboard.plugins.LeaderboardCorePlugin
import zio.IO

/**
  * Cross-platform base for the `RoleAppMain` launcher objects. Defaults
  * `pluginConfig` to the shared [[LeaderboardCorePlugin]] — sufficient
  * for the Scala.js in-browser simulation. The JVM service extends this
  * via [[JvmMainBase]] which adds the JVM-only server/postgres/docker
  * plugins.
  */
abstract class MainBase(
  activation: Activation,
  requiredRolesList: Vector[RoleArgs],
) extends RoleAppMain.LauncherBIO[IO] {

  override def requiredRoles(argv: RoleAppMain.ArgV): Vector[RoleArgs] = {
    requiredRolesList
  }

  override def pluginConfig: PluginConfig =
    PluginConfig.const(List(LeaderboardCorePlugin))

  protected override def roleAppBootOverrides(argv: RoleAppMain.ArgV): Module = super.roleAppBootOverrides(argv) ++ new ModuleDef {
    make[Activation].named("default").fromValue(defaultActivation ++ activation)
  }

  private def defaultActivation = Activation(Scene -> Scene.Provided)

}
