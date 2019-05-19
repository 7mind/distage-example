package com.github.ratoshniuk.izumi.distage.sample.users.services.production

import com.github.pshirshov.izumi.functional.bio.BIO
import com.github.pshirshov.izumi.functional.bio.BIO._
import com.github.pshirshov.izumi.functional.bio.BIO.catz._
import com.github.ratoshniuk.izumi.distage.sample.users.services.production.PostgresException.QueryException
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.syntax.connectionio._

trait PostgresConnector[F[+_, +_]] {
  def query[T](metaName: String)(query: ConnectionIO[T]): F[PostgresException, T]
}

object PostgresConnector {

  final class Impl[F[+_, +_]: BIO]
  (
    transactor: HikariTransactor[F[Throwable, ?]]
  ) extends PostgresConnector[F]  {

    override def query[T](metaName: String)(query: ConnectionIO[T]): F[PostgresException, T] = {
      query.transact(transactor)
        .sandbox
        .leftMap(_.toThrowable)
        .leftMap(QueryException(_))
    }
  }

}

sealed trait PostgresException {
  def msg: String
}

object PostgresException {

  final case class QueryException(msg: String) extends PostgresException
  object QueryException {
    def apply(err: Throwable): QueryException = new QueryException(err.getMessage)
  }

}
