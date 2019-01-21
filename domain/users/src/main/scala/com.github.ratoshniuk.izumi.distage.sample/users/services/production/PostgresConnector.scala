package com.github.ratoshniuk.izumi.distage.sample.users.services.production

import cats.effect.{ContextShift, IO => CIO}
import com.github.pshirshov.izumi.distage.config.annotations.ConfPath
import com.github.pshirshov.izumi.functional.bio.{BIO, BIOAsync}
import com.github.pshirshov.izumi.functional.bio.BIO._
import com.github.ratoshniuk.izumi.distage.sample.users.services.production.PostgresException.{QueryException, TimeoutException}
import com.zaxxer.hikari.HikariDataSource
import distage.Id
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.syntax.connectionio._
import logstage.IzLogger
import scalaz.zio.ExitResult.Cause
import scalaz.zio.FiberFailure

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait PostgresConnector[F[+_, +_]] {
  def query[T](metaName: String)(query: ConnectionIO[T]): F[PostgresException, T]

  def query[T](metaName: String, timeout: Duration)(query: ConnectionIO[T]): F[PostgresException, T]
}

object PostgresConnector {

  final class Impl[F[+_, +_]: BIO: BIOAsync]
  (
    @ConfPath("postgres") cfg: PostgresCfg
    , blockingIOExecutionContext: ExecutionContext@Id("blockingIO")
    , log: IzLogger
  )(implicit cs: ContextShift[CIO] @Id("global")) extends AutoCloseable with PostgresConnector[F]  {

    override def query[T](metaName: String)(q: ConnectionIO[T]): F[PostgresException, T] = {
      query[T](metaName, cfg.defTimeout)(q)
    }

    override def query[T](metaName: String, timeout: Duration)(query: ConnectionIO[T]): F[PostgresException, T] = {
      for {
        res <- {
          BIO[F].syncThrowable(query.transact(mkTransactor).unsafeRunSync())
            .leftMap(thr => QueryException(thr.getMessage ))
//            .flatMap(f => {
//              BIO[F] fromEither f.toRight(TimeoutException(s"Query $metaName timed out"))
//            })
        }
      } yield res
    }

    private[this] val mkTransactor: HikariTransactor[CIO] = {
      val ds = new HikariDataSource(cfg.hikariConfig)
      HikariTransactor.apply[CIO](ds, blockingIOExecutionContext, blockingIOExecutionContext)
    }

    override def close(): Unit = {
    }
  }

}


sealed trait PostgresException {
  def msg: String
}

object PostgresException {

  case class QueryException(msg: String) extends PostgresException

  case class TimeoutException(msg: String) extends PostgresException

}


import com.zaxxer.hikari.HikariConfig

import scala.concurrent.duration.FiniteDuration

final case class PostgresCfg(
                              jdbcDriver: String
                              , url: String
                              , user: String
                              , password: String
                              , defTimeout: FiniteDuration
                            ) {
  lazy val hikariConfig: HikariConfig = {
    val config = new HikariConfig()
    config.setJdbcUrl(url)
    config.setUsername(user)
    config.setPassword(password)
    config.setDriverClassName(jdbcDriver)
    config
  }
}
