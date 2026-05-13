package leaderboard.sim

import distage.StandardAxis.Repo
import distage.{Activation, Injector, ModuleDef, Roots}
import izumi.distage.modules.DefaultModule2
import izumi.logstage.distage.LogIO2Module
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
      // LogIO2[IO] needed by ProfileApi.
      include(LogIO2Module[IO]())
      // BIO typeclass instances (IO2, Async2, Primitives2, ...) used by the
      // dummy repos and the dispatcher.
      include(DefaultModule2[IO])
    }

    // Build the object graph and keep the resource open for the lifetime of
    // the page. We never finalize — the in-memory dummy state should live as
    // long as the JS module is loaded.
    val program: G[Nothing] =
      Injector.NoProxies[G]()
        .produce(module, Roots.target[LocalDispatcher[IO]], Activation(Repo -> Repo.Dummy))
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
