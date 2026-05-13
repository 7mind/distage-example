package leaderboard.plugins

import distage.StandardAxis.Repo
import distage.{ModuleDef, TagKK}
import leaderboard.api.{HttpApi, LadderApi, ProfileApi}
import leaderboard.dispatch.LocalDispatcher
import leaderboard.repo.{Ladder, Profiles}
import leaderboard.services.Ranks
import org.http4s.dsl.Http4sDsl

/**
  * Cross-compilable subset of the application wiring. Holds everything that
  * doesn't depend on JVM-only libraries (http4s ember server, doobie, JNA,
  * roles), so the same module can be assembled into a real http server on the
  * JVM and into an in-browser scala.js simulation.
  */
object LeaderboardCoreModule {

  def api[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
    // The `ladder` API
    make[LadderApi[F]]
    // The `profile` API
    make[ProfileApi[F]]

    // A set of all APIs
    many[HttpApi[F]]
      .weak[LadderApi[F]] // add ladder API as a _weak reference_
      .weak[ProfileApi[F]] // add profiles API as a _weak reference_

    make[Ranks[F]].from[Ranks.Impl[F]]

    makeTrait[Http4sDsl[F[Throwable, _]]]

    // Wraps the assembled HttpRoutes for direct in-process calls. Used both by
    // the http4s adapter on JVM and by the @JSExportTopLevel entrypoint in the
    // browser simulation.
    make[LocalDispatcher[F]].from[LocalDispatcher.Impl[F]]
  }

  def repoDummy[F[+_, +_]: TagKK]: ModuleDef = new ModuleDef {
    tag(Repo.Dummy)

    make[Ladder[F]].fromResource[Ladder.Dummy[F]]
    make[Profiles[F]].fromResource[Profiles.Dummy[F]]
  }
}
