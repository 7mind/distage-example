package leaderboard.plugins

import distage.{ModuleDef, Scene}
import izumi.distage.docker.Docker.DockerPort
import izumi.distage.docker.bundled.PostgresDocker
import izumi.distage.docker.modules.DockerSupportModule
import izumi.distage.plugins.{PluginBase, PluginDef}
import izumi.reflect.TagK
import leaderboard.config.PostgresPortCfg

object PostgresDockerPlugin {
  def apply[F[_]: TagK]: PluginBase = new PluginDef {
    include(dockerModule[F])
  }

  def dockerModule[F[_]: TagK]: ModuleDef = new ModuleDef {
    // only enable postgres docker when Scene axis is set to Managed
    tag(Scene.Managed)

    // add docker support dependencies
    include(DockerSupportModule[F])

    // launch postgres docker for tests
    make[PostgresDocker.Container]
      .fromResource(PostgresDocker.make[F])

    // spawned docker container port is randomized
    // to prevent conflicts, so make PostgresPortCfg
    // point to the new port. This will also
    // cause the container to start before
    // integration check is performed
    make[PostgresPortCfg].from {
      (docker: PostgresDocker.Container) =>
        val knownAddress = docker.availablePorts.first(DockerPort.TCP(5432))
        PostgresPortCfg(knownAddress.hostString, knownAddress.port)
    }
  }
}
