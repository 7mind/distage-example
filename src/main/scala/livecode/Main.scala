package livecode

import izumi.distage.framework.model.PluginSource
import izumi.distage.model.definition.Activation
import izumi.distage.model.definition.StandardAxis.Repo
import izumi.distage.plugins.load.PluginLoader.PluginConfig
import izumi.distage.roles.{RoleAppLauncher, RoleAppMain}
import izumi.fundamentals.platform.cli.model.raw.RawRoleParams
import livecode.code.LivecodeRole
import zio.Task
import zio.interop.catz._

object Main
  extends RoleAppMain.Default(
    launcher = new RoleAppLauncher.LauncherF[Task] {
      override val pluginSource = PluginSource(
        PluginConfig(
          debug            = false,
          packagesEnabled  = Seq("livecode.plugins"),
          packagesDisabled = Nil,
        )
      )
      override val requiredActivations = Activation(Repo -> Repo.Dummy)
    }
  ) {
  override val requiredRoles: Vector[RawRoleParams] = {
    Vector(RawRoleParams.empty(LivecodeRole.id))
  }
}
