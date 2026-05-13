package leaderboard.plugins

import distage.TagKK
import izumi.distage.plugins.PluginDef
import izumi.distage.roles.bundled.BundledRolesModule
import izumi.distage.roles.model.definition.RoleModuleDef
import izumi.fundamentals.platform.versions.Version
import leaderboard.{LadderRole, LeaderboardRole, ProfileRole}
import zio.IO

/**
  * Cross-platform `PluginDef` for the leaderboard application. Holds the
  * three application roles plus the shared API + dummy-repo wiring from
  * [[LeaderboardCoreModule]]. Active on both the JVM service and the
  * in-browser Scala.js simulation; the JVM half adds postgres/server bits
  * via [[LeaderboardServerPlugin]].
  */
object LeaderboardCorePlugin extends PluginDef {
  include(modules.roles[IO])
  include(LeaderboardCoreModule.api[IO])
  include(LeaderboardCoreModule.repoDummy[IO])

  object modules {
    def roles[F[+_, +_]: TagKK]: RoleModuleDef = new RoleModuleDef {
      // The `ladder` role
      makeRole[LadderRole[F]]

      // The `profile` role
      makeRole[ProfileRole[F]]

      // The composite `leaderboard` role that pulls in both `ladder` & `profile` roles
      makeRole[LeaderboardRole[F]]

      // Add bundled roles: `help` & `configwriter`
      include(BundledRolesModule[F[Throwable, _]](version = Version.parse("1.0.0")))
    }
  }
}
