package leaderboard.sql

import cats.effect.Async
import cats.syntax.applicativeError.*
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import leaderboard.model.QueryFailure

trait SQL[F[_]] {
  def execute[A](queryName: String)(conn: ConnectionIO[A]): F[A]
}

object SQL {
  final class Impl[F[_]: Async](
    transactor: Transactor[F]
  ) extends SQL[F] {
    override def execute[A](queryName: String)(conn: ConnectionIO[A]): F[A] = {
      transactor.trans
        .apply(conn)
        .handleErrorWith(ex => Async[F].raiseError(QueryFailure(queryName, ex)))
    }
  }
}
