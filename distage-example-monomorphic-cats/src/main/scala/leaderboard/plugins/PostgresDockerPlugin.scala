package leaderboard.plugins

import cats.effect.IO
import distage.Scene
import izumi.distage.docker.Docker.DockerPort
import izumi.distage.docker.bundled.PostgresDocker
import izumi.distage.docker.modules.DockerSupportModule
import izumi.distage.plugins.PluginDef
import leaderboard.config.PostgresPortCfg

object PostgresDockerPlugin extends PluginDef {
  // only enable postgres docker when Scene axis is set to Managed
  tag(Scene.Managed)

  // add docker support dependencies
  include(DockerSupportModule[IO])

  // launch postgres docker for tests
  make[PostgresDocker.Container]
    .fromResource(PostgresDocker.make[IO])

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
