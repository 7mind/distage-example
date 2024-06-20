package leaderboard.repo

import distage.Lifecycle
import doobie.postgres.implicits.*
import doobie.syntax.string.*
import izumi.functional.bio.{Applicative2, F, Monad2, Primitives2}
import leaderboard.model.{QueryFailure, Score, UserId}
import leaderboard.sql.SQL
import logstage.LogIO2

trait Ladder[F[_, _]] {
  def submitScore(userId: UserId, score: Score): F[QueryFailure, Unit]
  def getScores: F[QueryFailure, List[(UserId, Score)]]
}

object Ladder {
  final class Dummy[F[+_, +_]: Applicative2: Primitives2]
    extends Lifecycle.LiftF[F[Nothing, _], Ladder[F]](for {
      state <- F.mkRef(Map.empty[UserId, Score])
    } yield {
      new Ladder[F] {
        override def submitScore(userId: UserId, score: Score): F[Nothing, Unit] =
          state.update_(_ + (userId -> score))

        override def getScores: F[Nothing, List[(UserId, Score)]] =
          state.get.map(_.toList.sortBy(_._2)(Ordering[Score].reverse))
      }
    })

  final class Postgres[F[+_, +_]: Monad2](
    sql: SQL[F],
    log: LogIO2[F],
  ) extends Lifecycle.LiftF[F[Throwable, _], Ladder[F]](for {
      _ <- log.info(s"Creating Ladder table")
      _ <- sql.execute("ladder-ddl") {
        sql"""create table if not exists ladder (
             | user_id uuid not null,
             | score bigint not null,
             | primary key (user_id)
             |) without oids
             |""".stripMargin.update.run
      }
      res = new Ladder[F] {
        override def submitScore(userId: UserId, score: Score): F[QueryFailure, Unit] =
          sql
            .execute("submit-score") {
              sql"""insert into ladder (user_id, score) values ($userId, $score)
                   |on conflict (user_id) do update set
                   |  score = excluded.score
                   |""".stripMargin.update.run
            }.void

        override val getScores: F[QueryFailure, List[(UserId, Score)]] =
          sql.execute("get-leaderboard") {
            sql"""select user_id, score from ladder order by score DESC
                 |""".stripMargin.query[(UserId, Score)].to[List]
          }
      }
    } yield res)
}
