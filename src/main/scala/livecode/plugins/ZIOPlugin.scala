package livecode.plugins

import java.util.concurrent.ThreadPoolExecutor

import cats.effect.Blocker
import distage.Id
import distage.plugins.PluginDef
import izumi.distage.effect.modules.ZIODIEffectModule
import livecode._
import logstage.LogBIO
import zio.IO
import zio.interop.catz._
import zio.interop.catz.implicits._

import scala.concurrent.ExecutionContext

object ZIOPlugin extends ZIODIEffectModule with PluginDef {
  addImplicit[Bracket2[IO]]
  addImplicit[Async2[IO]]
  addImplicit[ContextShift2[IO]]
  addImplicit[Timer2[IO]]
  make[ConcurrentEffect2[IO]].from {
    runtime: zio.Runtime[Any] =>
      taskEffectInstance(runtime)
  }

  make[Blocker].from {
    pool: ThreadPoolExecutor @Id("zio.io") =>
      Blocker.liftExecutionContext(ExecutionContext.fromExecutorService(pool))
  }

  make[LogBIO[IO]].from(LogBIO.fromLogger[IO] _)
}
