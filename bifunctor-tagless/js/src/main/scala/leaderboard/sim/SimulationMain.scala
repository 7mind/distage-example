package leaderboard.sim

import distage.StandardAxis.Repo
import distage.{Activation, Injector, ModuleDef, Roots}
import izumi.distage.modules.DefaultModule2
import izumi.logstage.api.IzLogger
import izumi.logstage.distage.LogIO2Module
import izumi.logstage.sink.ConsoleSink
import leaderboard.dispatch.LocalDispatcher
import leaderboard.plugins.LeaderboardCoreModule
import zio.{IO, Promise as ZPromise, Runtime, Unsafe, ZIO}

import scala.concurrent.ExecutionContext
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * Entrypoint for the in-browser "perfect simulation" of the leaderboard
  * backend. Boots the same distage object graph as the JVM application but
  * with `Repo` axis pinned to `Dummy` (so all repository calls hit the
  * in-memory implementations), then exposes the resulting [[LocalDispatcher]]
  * to JavaScript as `window.LeaderboardSim.call(method, path, body)`.
  *
  * The dispatcher invokes the exact same `HttpRoutes` the JVM server uses,
  * so the front-end can address it with the same method/path/body it would
  * send via fetch() to the real backend.
  */
@JSExportTopLevel("LeaderboardSim")
object SimulationMain {

  private type G[A] = IO[Throwable, A]

  // A zio runtime — fine on JS since the default runtime does not assume any
  // JVM-specific scheduler.
  private val runtime: Runtime[Any] = Runtime.default

  // JS event loop — used only to materialize the cats Future returned by
  // `runtime.unsafe.runToFuture` into a JavaScript Promise.
  private implicit val ec: ExecutionContext = JSExecutionContext.queue

  // We resolve the dispatcher asynchronously (distage produce returns a
  // resource) and surface it through this promise. Every JS `call(...)`
  // awaits this before dispatching, so the front-end never sees an
  // uninitialised state.
  private val dispatcherReady: ZPromise[Throwable, LocalDispatcher[IO]] =
    Unsafe.unsafe(implicit u => runtime.unsafe.run(ZPromise.make[Throwable, LocalDispatcher[IO]]).getOrThrow())

  locally {
    val module = new ModuleDef {
      include(LeaderboardCoreModule.api[IO])
      include(LeaderboardCoreModule.repoDummy[IO])
      // LogIO2[IO] (needed by ProfileApi) + a console logger. We use
      // `SimpleConsoleSink` instead of the default `ColoredConsoleSink`
      // because the latter probes `process.env` for terminal-color detection
      // on init, which doesn't exist in the browser.
      include(LogIO2Module[IO]())
      make[IzLogger].fromValue(IzLogger(sink = ConsoleSink.SimpleConsoleSink))
      // BIO + cats-effect typeclass instances for ZIO. When zio-interop-cats
      // is on the classpath, this resolves to `DefaultModule.forZIOPlusCats`
      // which binds `cats.effect.Async[Task]` etc. — the dispatcher needs it.
      include(DefaultModule2[IO])
    }

    // Build the object graph and keep the resource open for the lifetime of
    // the page. We never finalize — the in-memory dummy state should live as
    // long as the JS module is loaded.
    val program: G[Nothing] =
      Injector.NoProxies[G]()
        // `Roots.Everything` instead of `Roots.target[LocalDispatcher]` because
        // `LeaderboardCoreModule.api` adds `LadderApi`/`ProfileApi` to the
        // `Set[HttpApi[F]]` as *weak* references — they only join the set if
        // they're independently reachable through the plan. The JVM app pulls
        // them in via roles; here we have no roles, so we ask the planner to
        // include every binding that's not pinned out by the activation.
        .produce(module, Roots.Everything, Activation(Repo -> Repo.Dummy))
        .use {
          locator =>
            dispatcherReady.succeed(locator.get[LocalDispatcher[IO]]) *> ZIO.never
        }
        .catchAll(err => dispatcherReady.fail(err) *> ZIO.never)

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
