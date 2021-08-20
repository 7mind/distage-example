package leaderboard.sql

import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import izumi.functional.bio.Panic2
import izumi.functional.bio.catz.*
import leaderboard.model.QueryFailure

trait SQL[F[_, _]] {
  def execute[A](queryName: String)(conn: ConnectionIO[A]): F[QueryFailure, A]
}

object SQL {
  final class Impl[F[+_, +_]: Panic2](
    transactor: Transactor[F[Throwable, _]]
  ) extends SQL[F] {
    override def execute[A](queryName: String)(conn: ConnectionIO[A]): F[QueryFailure, A] = {
      transactor.trans
        .apply(conn)
        .leftMap(QueryFailure(queryName, _))
    }
  }
}
