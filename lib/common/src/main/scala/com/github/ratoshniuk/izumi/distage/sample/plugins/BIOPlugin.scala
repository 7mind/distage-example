package com.github.ratoshniuk.izumi.distage.sample.plugins

import java.util.concurrent.{LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

import cats.effect._
import com.github.pshirshov.izumi.distage.monadic.modules.ZioDIEffectModule
import com.github.pshirshov.izumi.distage.plugins.PluginDef
import com.github.pshirshov.izumi.distage.roles.services.ResourceRewriter
import com.github.pshirshov.izumi.functional.bio._
import distage.Id
import logstage.{IzLogger, LogBIO}
import scalaz.zio
import scalaz.zio.interop.Util
import scalaz.zio.interop.catz._
import scalaz.zio.{IO, Task, ZIO}

import scala.concurrent.{ExecutionContext, Future}

class BIOPlugin
  extends ZioDIEffectModule
  with PluginDef {

  make[zio.clock.Clock].from(zio.clock.Clock.Live)

  make[BIOError[IO]].using[BIO[IO]]
  make[BIOAsync[IO]].from {
    implicit clock: zio.clock.Clock =>
      BIOAsync[IO]
  }
  addImplicit[BIO[IO]]
  addImplicit[BIOTransZio[IO]]
  addImplicit[BIOFork[IO]]
  addImplicit[BIOFromFuture[IO]]
  addImplicit[SyncSafe2[IO]]

  addImplicit[ContextShift[IO[Throwable, ?]]]
  addImplicit[Bracket[IO[Throwable, ?], Throwable]]
  addImplicit[Sync[IO[Throwable, ?]]]
  addImplicit[Async[IO[Throwable, ?]]]

  make[LogBIO[IO]].from(LogBIO.fromLogger(_: IzLogger)(_: SyncSafe2[IO]))

  make[ThreadPoolExecutor].named("zio-es").fromResource {
    logger: IzLogger =>
      val cores = Runtime.getRuntime.availableProcessors.max(2)

      ResourceRewriter.fromExecutorService(logger,
        new ThreadPoolExecutor(cores, cores, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue[Runnable]))
  }

  make[ExecutionContext].named("blockingIO").from {
    es: ThreadPoolExecutor @Id("zio.pool.io") =>
      ExecutionContext.fromExecutorService(es): ExecutionContext
  }

}

trait BIOFromFuture[F[_, _]] {
  def fromFuture[A](f: F[Throwable, Future[A]]): F[Throwable, A]
}

object BIOFromFuture {
  def apply[F[_, _]: BIOFromFuture]: BIOFromFuture[F] = implicitly

  implicit val bioFromFutureIO: BIOFromFuture[IO] = new BIOFromFuture[IO] {
    override def fromFuture[A](f: Task[Future[A]]): Task[A] = {
      for {
        ec <- ZIO.descriptor.map(_.executor.asEC)
        res <- Util.fromFuture(ec)(f)
      } yield res
    }
  }
}
