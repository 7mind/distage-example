package leaderboard.repo

import distage.DIResource
import doobie.postgres.implicits._
import doobie.syntax.string._
import izumi.functional.bio.{BIOApplicative, BIOMonad, BIOPrimitives, F}
import leaderboard.model.{QueryFailure, UserId, UserProfile}
import leaderboard.sql.SQL
import logstage.LogBIO

trait Profiles[F[_, _]] {
  def setProfile(userId: UserId, profile: UserProfile): F[QueryFailure, Unit]
  def getProfile(userId: UserId): F[QueryFailure, Option[UserProfile]]
}

object Profiles {
  final class Dummy[F[+_, +_]: BIOApplicative: BIOPrimitives]
    extends DIResource.LiftF[F[Nothing, ?], Profiles[F]](for {
      state <- F.mkRef(Map.empty[UserId, UserProfile])
    } yield {
      new Profiles[F] {
        override def setProfile(userId: UserId, profile: UserProfile): F[Nothing, Unit] =
          state.update_(_ + (userId -> profile))

        override def getProfile(userId: UserId): F[Nothing, Option[UserProfile]] =
          state.get.map(_.get(userId))
      }
    })

  final class Postgres[F[+_, +_]: BIOMonad](
    sql: SQL[F],
    log: LogBIO[F],
  ) extends DIResource.LiftF[F[Throwable, ?], Profiles[F]](for {
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
      override def setProfile(userId: UserId, profile: UserProfile): F[QueryFailure, Unit] = {
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

      override def getProfile(userId: UserId): F[QueryFailure, Option[UserProfile]] = {
        sql.execute("get-profile") {
          sql"""select name, description from profiles
               |where user_id = $userId
               |""".stripMargin.query[UserProfile].option
        }
      }
    })
}
