package leaderboard.sim

import distage.Activation
import distage.StandardAxis.Repo
import izumi.fundamentals.platform.cli.model.RoleArgs
import leaderboard.dispatch.LocalDispatcher
import leaderboard.{LeaderboardRole, MainBase}
import zio.{IO, Promise as ZPromise, Runtime, Unsafe, ZIO}

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * Entrypoint for the in-browser "perfect simulation" of the leaderboard
  * backend. Boots the same distage object graph as the JVM application
  * by running `RoleAppMain.replLocator(":leaderboard")` against the
  * shared [[MainBase]], pinning `Repo -> Dummy` so the JS half wires the
  * in-memory dummy repositories. Surfaces the resulting
  * [[LocalDispatcher]] to JavaScript as
  * `window.LeaderboardSim.call(method, path, body)`.
  *
  * `MainBase` is `abstract` and inherits a `main` method whose JS
  * return type is `Future[Unit]`. A top-level `object` cannot extend
  * such a class (Scala 2.13 rejects "not a valid main method"). The
  * workaround here is to instantiate an anonymous concrete `MainBase`
  * inline so the JS entrypoint stays a plain `@JSExportTopLevel` object
  * without inheriting that `main`.
  */
@JSExportTopLevel("LeaderboardSim")
object SimulationMain {

  private type G[A] = IO[Throwable, A]

  // Suppress the framework's terminal-color probe before anything else runs.
  // `RoleAppMain`'s early logger uses `ColoredConsoleSink`, whose rendering
  // policy consults `IzPlatform.terminalColorsEnabled` on the first log entry
  // — that path calls `process.env.DISPLAY` via Scala.js's Node-env shim, and
  // a browser bundle has no `process` global. Setting the well-known
  // system property short-circuits the probe at the first guard in
  // `_terminalColorsEnabled` (see `izumi.fundamentals.platform.IzPlatform`).
  System.setProperty("izumi.platform.disable-terminal-colors", "true")

  // A zio runtime — fine on JS since the default runtime does not assume any
  // JVM-specific scheduler.
  private val runtime: Runtime[Any] = Runtime.default

  // JS event loop — used only to materialize the cats Future returned by
  // `runtime.unsafe.runToFuture` into a JavaScript Promise.
  private implicit val ec: ExecutionContext = JSExecutionContext.queue

  // We resolve the dispatcher asynchronously and surface it through this
  // promise. Every JS `call(...)` awaits this before dispatching, so the
  // front-end never sees an uninitialised state.
  private val dispatcherReady: ZPromise[Throwable, LocalDispatcher[IO]] =
    Unsafe.unsafe(implicit u => runtime.unsafe.run(ZPromise.make[Throwable, LocalDispatcher[IO]]).getOrThrow())

  // Anonymous concrete `MainBase` pinned to `Repo -> Dummy`. Cannot live
  // at top level: the inherited `main(args)` returns `Future[Unit]` on
  // JS and Scala 2.13 rejects objects with non-Unit main methods.
  private val launcher: MainBase = new MainBase(
    activation        = Activation(Repo -> Repo.Dummy),
    requiredRolesList = Vector(RoleArgs(LeaderboardRole.id)),
  ) {}

  locally {
    val program: G[Nothing] = launcher
      .replLocator(s":${LeaderboardRole.id}")
      .flatMap(locator => dispatcherReady.succeed(locator.get[LocalDispatcher[IO]]))
      .catchAll(err => dispatcherReady.fail(err)) *> ZIO.never

    Unsafe.unsafe(implicit u => runtime.unsafe.fork(program))
  }

  /**
    * Dispatch a request to the simulated backend.
    *
    * @return a JS promise resolving to `{ status: Int, body: String }`.
    */
  @JSExport
  def call(method: String, path: String, body: String): js.Promise[js.Dynamic] = {
    val program: G[js.Dynamic] = for {
      dispatcher <- dispatcherReady.await
      response   <- dispatcher.call(method, path, body)
    } yield js.Dynamic.literal(status = response.status, body = response.body)

    Unsafe.unsafe(implicit u => runtime.unsafe.runToFuture(program).toJSPromise)
  }
}
