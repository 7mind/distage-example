package example.sql

import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import example.model.QueryFailure
import izumi.functional.bio.BIOPanic
import izumi.functional.bio.catz._

trait SQL[F[_, _]] {
  def execute[A](queryName: String)(conn: ConnectionIO[A]): F[QueryFailure, A]
}

object SQL {
  final class Impl[F[+_, +_]: BIOPanic](
    transactor: Transactor[F[Throwable, ?]]
  ) extends SQL[F] {
    override def execute[A](queryName: String)(conn: ConnectionIO[A]): F[QueryFailure, A] = {
      transactor.trans
        .apply(conn)
        .leftMap(QueryFailure(queryName, _))
    }
  }
}
