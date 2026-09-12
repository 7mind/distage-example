package leaderboard.sql

import cats.effect.IO
import org.typelevel.doobie.free.connection.ConnectionIO
import org.typelevel.doobie.util.transactor.Transactor
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
