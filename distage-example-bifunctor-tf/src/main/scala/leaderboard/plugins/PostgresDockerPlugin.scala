package leaderboard.plugins

import distage.{ModuleDef, Scene}
import izumi.distage.docker.Docker.DockerPort
import izumi.distage.docker.bundled.PostgresDocker
import izumi.distage.docker.modules.DockerSupportModule
import izumi.distage.plugins.PluginDef
import izumi.reflect.TagKK
import leaderboard.config.PostgresPortCfg
import zio.IO

object PostgresDockerPlugin extends PluginDef {
  include(dockerModule[IO])

  def dockerModule[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
    // only enable postgres docker when Scene axis is set to Managed
    tag(Scene.Managed)

    // add docker support dependencies
    include(DockerSupportModule[F[Throwable, _]])

    // launch postgres docker for tests
    make[PostgresDocker.Container]
      .fromResource(PostgresDocker.make[F[Throwable, _]])

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
