package com.github.ratoshniuk.izumi.distage.sample

import com.github.pshirshov.izumi.distage.plugins.load.PluginLoaderDefaultImpl.PluginConfig
import com.github.pshirshov.izumi.distage.roles.impl.ScoptRoleApp
import com.github.pshirshov.izumi.distage.roles.launcher.RoleAppBootstrapStrategy.Using
import com.github.pshirshov.izumi.distage.roles.launcher.{RoleApp, RoleAppBootstrapStrategy}

class DistageApp extends RoleApp with ScoptRoleApp  {
  override val pluginConfig = PluginConfig(
    debug = false
    , packagesEnabled = Seq("com.github.ratoshniuk.izumi.distage.sample.plugins")
    , packagesDisabled = Seq.empty
  )



  override protected def using: Seq[RoleAppBootstrapStrategy.Using] = {
    Seq(
      Using("izumi-workshop-01-master", classOf[DistageApp])
    )
  }
}


