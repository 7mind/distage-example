package leaderboard.plugins

import izumi.distage.docker.Docker.DockerPort
import izumi.distage.docker.examples.PostgresDocker
import izumi.distage.docker.modules.DockerSupportModule
import izumi.distage.plugins.PluginDef
import leaderboard.axis.Services
import leaderboard.config.PostgresPortCfg
import zio.Task

object PostgresDockerPlugin extends PluginDef {
  // only enable postgres docker when Service axis is set to Docker
  tag(Services.Docker)

  // add docker support dependencies
  include(DockerSupportModule[Task])

  // launch postgres docker for tests
  make[PostgresDocker.Container]
    .fromResource(PostgresDocker.make[Task])

  // spawned docker container port is randomized
  // to prevent conflicts, so make PostgresPortCfg
  // point to the new port. This will also
  // cause the container to start before
  // integration check is performed
  make[PostgresPortCfg].from {
    docker: PostgresDocker.Container =>
      val knownAddress = docker.availablePorts.availablePorts(DockerPort.TCP(5432)).head
      PostgresPortCfg(knownAddress.hostV4, knownAddress.port)
  }
}
