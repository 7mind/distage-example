package sample.repo

import distage.DIResource
import doobie.postgres.implicits._
import doobie.syntax.string._
import izumi.functional.bio.{BIO, BIOApplicative, BIOPrimitives, F}
import sample.model.{QueryFailure, UserId, UserProfile}
import sample.sql.SQL
import logstage.LogBIO

trait Profiles[F[_, _]] {
  def setProfile(userId: UserId, profile: UserProfile): F[QueryFailure, Unit]
  def getProfile(userId: UserId): F[QueryFailure, Option[UserProfile]]
}

object Profiles {
  final class Dummy[F[+_, +_]: BIO: BIOPrimitives]
    extends DIResource.LiftF[F[Throwable, ?], Profiles[F]](
      F.mkRef(Map.empty[UserId, UserProfile]).map {
        state =>
          new Profiles[F] {
            override def setProfile(userId: UserId, profile: UserProfile): F[Nothing, Unit] =
              state.update_(_ + (userId -> profile))

            override def getProfile(userId: UserId): F[Nothing, Option[UserProfile]] =
              state.get.map(_.get(userId))
          }
      }
    )

  final class Postgres[F[+_, +_]: BIOApplicative](
    sql: SQL[F],
    log: LogBIO[F],
  ) extends DIResource.Self[F[Throwable, ?], Profiles[F]]
    with Profiles[F] {

    override val acquire: F[QueryFailure, Unit] = {
      log.info("Creating Profile table") *>
      sql
        .execute("ddl-profiles") {
          sql"""create table if not exists profiles (
               |  user_id uuid not null,
               |  name text not null,
               |  description text not null,
               |  primary key (user_id)
               |) without oids
               |""".stripMargin.update.run
        }.void
    }
    override val release: F[QueryFailure, Unit] = F.unit

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
  }
}
