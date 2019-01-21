package com.github.ratoshniuk.izumi.distage.sample.users.services.production

import cats.effect.{ContextShift, Effect}
import com.github.pshirshov.izumi.distage.config.annotations.ConfPath
import com.github.pshirshov.izumi.functional.bio.BIO._
import com.github.pshirshov.izumi.functional.bio.{BIO, BIOAsync}
import com.github.ratoshniuk.izumi.distage.sample.users.services.production.PostgresConnector.PostgresException
import com.github.ratoshniuk.izumi.distage.sample.users.services.production.PostgresConnector.PostgresException.{QueryException, TimeoutException}
import com.zaxxer.hikari.HikariDataSource
import distage.Id
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.syntax.connectionio._
import logstage.IzLogger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

trait PostgresConnector[F[_, _]] {
  def query[T](metaName: String)(query: ConnectionIO[T]): F[PostgresException, T]

  def query[T](metaName: String, timeout: Duration)(query: ConnectionIO[T]): F[PostgresException, T]
}

object PostgresConnector {
  final class Impl[F[+ _, + _] : BIO: BIOAsync]
  (
    cfg: PostgresCfg @ConfPath("postgres")
    , blockingIOExecutionContext: ExecutionContext @Id("blockingIO")
    , log: IzLogger
  )(implicit cs: ContextShift[F[Throwable, ?]], e: Effect[F[Throwable, ?]]) extends PostgresConnector[F] {

    override def query[T](metaName: String)(query: ConnectionIO[T]): F[PostgresException, T] = {
      this.query(metaName, cfg.defTimeout)(query)
    }

    override def query[T](metaName: String, timeout: Duration)(query: ConnectionIO[T]): F[PostgresException, T] = {
      for {
        _ <- BIO[F].sync(log.info(s"Performing query $metaName"))
        transactor <- mkTransactor
        res <-  {
          query.transact(transactor)
            .sandboxWith(_.catchAll {
              case Left(errors) =>
//                val failure = errors.map(Cause.unchecked).reduceOption(_ ++ _).map(FiberFailure(_))
                val stackTrace = ""
                BIO[F].sync(log.error(s"Uncaught defect from doobie: $stackTrace")) *>
                  BIO[F].fail(Right(QueryException(s"Query $metaName failed due to unhandled defect: $stackTrace")))
              case Right(exc) =>
                BIO[F].fail(Right(QueryException(s"Query $metaName failed due to exception: $exc")))

            })
            .timeout(timeout)
            .flatMap(f => {
                BIO[F] fromEither f.toRight(TimeoutException(s"Query $metaName timed out"))
            })
        }
      } yield res
    }

    private[this] val mkTransactor: F[Nothing, HikariTransactor[F[Throwable, ?]]] = {
      val ds = new HikariDataSource(cfg.hikariConfig)
      BIO[F].sync(ds).map(HikariTransactor(_, blockingIOExecutionContext, blockingIOExecutionContext))
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
}
