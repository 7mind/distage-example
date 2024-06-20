package leaderboard.sql

import cats.effect.IO
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import leaderboard.model.QueryFailure

trait SQL {
  def execute[A](queryName: String)(conn: ConnectionIO[A]): IO[A]
}

object SQL {
  final class Impl(
    transactor: Transactor[IO]
  ) extends SQL {
    override def execute[A](queryName: String)(conn: ConnectionIO[A]): IO[A] = {
      transactor.trans
        .apply(conn)
        .handleErrorWith(ex => IO.raiseError(QueryFailure(queryName, ex)))
    }
  }
}
