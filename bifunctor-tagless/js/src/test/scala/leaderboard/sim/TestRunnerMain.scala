package leaderboard.sim

import leaderboard.{LadderTestDummy, ProfilesTestDummy, RanksTestDummy}
import org.scalatest.events.{Event, TestCanceled, TestFailed, TestPending, TestSucceeded}
import org.scalatest.{Args, CompositeStatus, ConfigMap, Filter, Reporter, Stopper, Tracker}

import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext
import scala.scalajs.js
import scala.scalajs.js.JSConverters.*
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * In-browser scalatest runner for the cross-compiled `*TestDummy`
  * suites. Instantiates each suite, drives it with a custom
  * `Reporter` that records events into a flat `js.Array`, and surfaces
  * the array via `js.Promise` once all suites have completed.
  *
  * Bundle name: `test-main.js` (see the `copySimJs` task).
  *
  * Frontend contract:
  *   `LeaderboardTestRunner.run(): Promise<Array<{
  *      kind: "passed" | "failed" | "cancelled" | "pending",
  *      suite: string,
  *      test: string,
  *      duration?: number,
  *      message?: string,
  *   }>>`
  */
@JSExportTopLevel("LeaderboardTestRunner")
object TestRunnerMain {

  // Same color-probe short-circuit `SimulationMain` uses — the test bundle
  // boots the same framework path (via testkit's `SpecZIO`) and would
  // otherwise hit `process.env.DISPLAY` at first log render.
  System.setProperty("izumi.platform.disable-terminal-colors", "true")

  private implicit val ec: scala.concurrent.ExecutionContext = JSExecutionContext.queue

  private def durationJs(opt: Option[Long]): js.Any =
    opt.fold(null.asInstanceOf[js.Any])(d => d.toDouble: js.Any)

  @JSExport
  def run(): js.Promise[js.Array[js.Object]] = {
    val recorded = scala.collection.mutable.ListBuffer.empty[js.Object]
    val reporter = new Reporter {
      override def apply(event: Event): Unit = event match {
        case e: TestSucceeded =>
          recorded += js.Dynamic.literal(
            kind     = "passed",
            suite    = e.suiteName,
            test     = e.testName,
            duration = durationJs(e.duration),
          )
        case e: TestFailed =>
          recorded += js.Dynamic.literal(
            kind     = "failed",
            suite    = e.suiteName,
            test     = e.testName,
            duration = durationJs(e.duration),
            message  = e.message,
          )
        case e: TestCanceled =>
          recorded += js.Dynamic.literal(
            kind     = "cancelled",
            suite    = e.suiteName,
            test     = e.testName,
            duration = durationJs(e.duration),
            message  = e.message,
          )
        case e: TestPending =>
          recorded += js.Dynamic.literal(
            kind  = "pending",
            suite = e.suiteName,
            test  = e.testName,
          )
        case _ => ()
      }
    }

    val args = new Args(
      reporter    = reporter,
      stopper     = Stopper.default,
      filter      = Filter.default,
      configMap   = ConfigMap.empty,
      distributor = None,
      tracker     = new Tracker,
    )

    val suites: List[org.scalatest.Suite] = List(
      new LadderTestDummy,
      new ProfilesTestDummy,
      new RanksTestDummy,
    )
    val statuses = suites.map(_.run(None, args))
    val combined = new CompositeStatus(statuses.toSet)

    val promise = Promise[js.Array[js.Object]]()
    combined.whenCompleted(_ => promise.success(recorded.toList.toJSArray))
    promise.future.toJSPromise
  }
}
