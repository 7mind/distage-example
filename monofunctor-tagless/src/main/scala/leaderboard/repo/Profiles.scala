package leaderboard.repo

import cats.Monad
import cats.effect.{Concurrent, Ref}
import cats.syntax.all.*
import distage.Lifecycle
import doobie.postgres.implicits.*
import doobie.syntax.string.*
import leaderboard.model.{UserId, UserProfile}
import leaderboard.sql.SQL
import logstage.LogIO

trait Profiles[F[_]] {
  def setProfile(userId: UserId, profile: UserProfile): F[Unit]
  def getProfile(userId: UserId): F[Option[UserProfile]]
}

object Profiles {
  final class Dummy[F[_]: Concurrent]
    extends Lifecycle.LiftF[F, Profiles[F]](for {
      state <- Ref.of(Map.empty[UserId, UserProfile])
    } yield {
      new Profiles[F] {
        override def setProfile(userId: UserId, profile: UserProfile): F[Unit] =
          state.update(_ + (userId -> profile))

        override def getProfile(userId: UserId): F[Option[UserProfile]] =
          state.get.map(_.get(userId))
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
