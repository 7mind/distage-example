package com.github.ratoshniuk.izumi.distage.sample

import com.github.pshirshov.izumi.distage.model.definition.StandardAxis.Repo
import com.github.pshirshov.izumi.distage.model.definition.{Axis, AxisBase}
import com.github.pshirshov.izumi.distage.plugins.load.PluginLoader.PluginConfig
import com.github.pshirshov.izumi.distage.roles.model.meta.LibraryReference
import com.github.pshirshov.izumi.distage.roles.{BootstrapConfig, RoleAppLauncher}
import scalaz.zio.IO
import scalaz.zio.interop.catz._

class DistageApp extends RoleAppLauncher.LauncherF[IO[Throwable, ?]] {
  override val bootstrapConfig = BootstrapConfig(
    PluginConfig(
      debug = false
    , packagesEnabled = Seq("com.github.ratoshniuk.izumi.distage.sample.plugins")
    , packagesDisabled = Seq.empty
    )
  )

  override protected def referenceLibraryInfo: Seq[LibraryReference] = {
    Seq(
      LibraryReference("izumi-workshop-01-master", classOf[DistageApp])
    )
  }

  override protected def defaultActivations: Map[AxisBase, Axis.AxisValue] =
    Map(Repo -> Repo.Prod)
}

object DistageApp extends DistageApp
