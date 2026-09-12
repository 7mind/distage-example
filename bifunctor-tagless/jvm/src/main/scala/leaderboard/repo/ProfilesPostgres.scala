package leaderboard.repo

import distage.Lifecycle
import org.typelevel.doobie.postgres.implicits.*
import org.typelevel.doobie.implicits.*
import izumi.functional.bio.Monad2
import leaderboard.model.{QueryFailure, UserId, UserProfile}
import leaderboard.sql.SQL
import logstage.LogIO2

// Postgres-backed implementation of [[Profiles]]. Lives JVM-side because doobie
// is not cross-built to scala.js.
final class ProfilesPostgres[F[+_, +_]: Monad2](
  sql: SQL[F],
  log: LogIO2[F],
) extends Lifecycle.LiftF[F[Throwable, _], Profiles[F]](for {
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
