package livecode.plugins

import java.util.concurrent.ThreadPoolExecutor

import cats.effect.Blocker
import distage.Id
import distage.plugins.PluginDef
import izumi.distage.monadic.modules.ZIODIEffectModule
import izumi.functional.bio.BIOPrimitives
import livecode.{Async2, Bracket2, ConcurrentEffect2, ContextShift2, Timer2}
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
  make[ConcurrentEffect2[IO]].from((runtime: zio.Runtime[Any]) => taskEffectInstance(runtime))

  addImplicit[BIOPrimitives[IO]]

  make[Blocker].from {
    pool: ThreadPoolExecutor @Id("zio.io") =>
      Blocker.liftExecutionContext(ExecutionContext.fromExecutorService(pool))
  }

  make[LogBIO[IO]].from(LogBIO.fromLogger[IO] _)
}
