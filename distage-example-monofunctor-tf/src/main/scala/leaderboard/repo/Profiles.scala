package leaderboard.repo

import distage.Lifecycle
import cats.{Applicative, Monad}
import cats.implicits.*
import doobie.postgres.implicits.*
import doobie.syntax.string.*
import leaderboard.model.{UserId, UserProfile}
import leaderboard.sql.SQL
import logstage.LogIO

import scala.collection.concurrent.TrieMap

trait Profiles[F[_]] {
  def setProfile(userId: UserId, profile: UserProfile): F[Unit]
  def getProfile(userId: UserId): F[Option[UserProfile]]
}

object Profiles {
  final class Dummy[F[_]: Applicative]
    extends Lifecycle.LiftF[F, Profiles[F]](for {
      state <- Applicative[F].pure(TrieMap.empty[UserId, UserProfile])
    } yield {
      new Profiles[F] {
        override def setProfile(userId: UserId, profile: UserProfile): F[Unit] =
          Applicative[F].pure(state.update(userId, profile))

        override def getProfile(userId: UserId): F[Option[UserProfile]] =
          Applicative[F].pure(state.get(userId))
      }
    })

  final class Postgres[F[_]: Monad](
    sql: SQL[F],
    log: LogIO[F],
  ) extends Lifecycle.LiftF[F, Profiles[F]](for {
      _ <- log.info("Creating Profile table")
      _ <- sql.execute("ddl-profiles") {
        sql"""create table if not exists profiles (
             |  user_id uuid not null,
             |  name text not null,
             |  description text not null,
             |  primary key (user_id)
             |) without oids
             |""".stripMargin.update.run
      }
    } yield new Profiles[F] {
      override def setProfile(userId: UserId, profile: UserProfile): F[Unit] = {
        sql
          .execute("set-profile") {
            sql"""insert into profiles (user_id, name, description)
                 |values ($userId, ${profile.name}, ${profile.description})
                 |on conflict (user_id) do update set
                 |  name = excluded.name,
                 |  description = excluded.description
                 |""".stripMargin.update.run
          }.void
      }

      override def getProfile(userId: UserId): F[Option[UserProfile]] = {
        sql.execute("get-profile") {
          sql"""select name, description from profiles
               |where user_id = $userId
               |""".stripMargin.query[UserProfile].option
        }
      }
    })
}
