package leaderboard.repo

import cats.effect.IO
import distage.Lifecycle
import doobie.postgres.implicits.*
import doobie.syntax.string.*
import leaderboard.model.{UserId, UserProfile}
import leaderboard.sql.SQL
import logstage.LogIO

import scala.collection.concurrent.TrieMap

trait Profiles {
  def setProfile(userId: UserId, profile: UserProfile): IO[Unit]
  def getProfile(userId: UserId): IO[Option[UserProfile]]
}

object Profiles {
  final class Dummy
    extends Lifecycle.LiftF[IO, Profiles](for {
      state <- IO.pure(TrieMap.empty[UserId, UserProfile])
    } yield {
      new Profiles {
        override def setProfile(userId: UserId, profile: UserProfile): IO[Unit] =
          IO.pure(state.update(userId, profile))

        override def getProfile(userId: UserId): IO[Option[UserProfile]] =
          IO.pure(state.get(userId))
      }
    })

  final class Postgres(
    sql: SQL,
    log: LogIO[IO],
  ) extends Lifecycle.LiftF[IO, Profiles](for {
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
    } yield new Profiles {
      override def setProfile(userId: UserId, profile: UserProfile): IO[Unit] = {
        sql
          .execute("set-profile") {
            sql"""insert into profiles (user_id, name, description)
                 |values ($userId, ${profile.name}, ${profile.description})
                 |on conflict (user_id) do update set
                 |  name = excluded.name,
                 |  description = excluded.description
                 |""".stripMargin.update.run
          }
      }.void

      override def getProfile(userId: UserId): IO[Option[UserProfile]] = {
        sql.execute("get-profile") {
          sql"""select name, description from profiles
               |where user_id = $userId
               |""".stripMargin.query[UserProfile].option
        }
      }
    })
}
