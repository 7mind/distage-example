package livecode

import java.net.URI
import java.util.UUID

import cats.effect.{Async, Blocker, ContextShift, Resource}
import distage.DIResource
import distage.DIResource.DIResourceBase
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import io.circe.{Codec, derivation}
import izumi.distage.framework.model.IntegrationCheck
import izumi.distage.roles.model.{RoleDescriptor, RoleService}
import izumi.functional.bio.catz.{BIOToBracket, BIOToSync}
import izumi.functional.bio.{BIO, BIOApplicative, BIOFunctor, BIOMonad, BIOPanic, BIOPrimitives, BIORef, F}
import izumi.fundamentals.platform.cli.model.raw.RawEntrypointParams
import izumi.fundamentals.platform.integration.{PortCheck, ResourceCheck}
import logstage.LogBIO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder

import scala.annotation.unused

object code {
  type UserId = UUID
  type Score  = Long

  final case class UserProfile(
    name: String,
    description: String,
  )
  object UserProfile {
    implicit val codec: Codec.AsObject[UserProfile] = derivation.deriveCodec
  }

  final case class RankedProfile(
    name: String,
    description: String,
    rank: Int,
    score: Score,
  )
  object RankedProfile {
    implicit val codec: Codec.AsObject[RankedProfile] = derivation.deriveCodec
  }

  trait Ladder[F[_, _]] {
    def submitScore(userId: UserId, score: Score): F[QueryFailure, Unit]
    def getScores: F[QueryFailure, List[(UserId, Score)]]
  }

  final case class QueryFailure(queryName: String, cause: Throwable)
    extends RuntimeException(
      s"""Query "$queryName" failed with ${cause.getMessage}""",
      cause,
    )

  trait Profiles[F[_, _]] {
    def setProfile(userId: UserId, profile: UserProfile): F[QueryFailure, Unit]
    def getProfile(userId: UserId): F[QueryFailure, Option[UserProfile]]
  }

  trait Ranks[F[_, _]] {
    def getRank(userId: UserId): F[QueryFailure, Option[RankedProfile]]
  }

  final class LadderDummy[F[+_, +_]: BIO: BIOPrimitives]
    extends DIResource.LiftF[F[Throwable, ?], Ladder[F]](
      F.mkRef(Map.empty[UserId, Score]).map(new LadderDummy.Impl(_))
    )

  object LadderDummy {

    final class Impl[F[+_, +_]: BIOFunctor](
      state: BIORef[F, Map[UserId, Score]],
    ) extends Ladder[F] {
      override def submitScore(userId: UserId, score: Score): F[Nothing, Unit] =
        state.update_(_ + (userId -> score))

      override val getScores: F[Nothing, List[(UserId, Score)]] =
        state.get.map(_.toList.sortBy(_._2)(Ordering[Score].reverse))
    }
  }

  final class ProfilesDummy[F[+_, +_]: BIO: BIOPrimitives]
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

  object Ranks {
    final class Impl[F[+_, +_]: BIOMonad](
      ladder: Ladder[F],
      profiles: Profiles[F],
    ) extends Ranks[F] {

      override def getRank(userId: UserId): F[QueryFailure, Option[RankedProfile]] = {
        for {
          maybeProfile <- profiles.getProfile(userId)
          scores       <- ladder.getScores
          res = for {
            profile <- maybeProfile
            rank    = scores.indexWhere(_._1 == userId) + 1
            score   <- scores.find(_._1 == userId).map(_._2)
          } yield RankedProfile(
            name        = profile.name,
            description = profile.description,
            rank        = rank,
            score       = score,
          )
        } yield res
      }
    }
  }

  trait SQL[F[_, _]] {
    def execute[A](queryName: String)(conn: ConnectionIO[A]): F[QueryFailure, A]
  }

  object SQL {
    final class Impl[F[+_, +_]: BIOPanic](
      transactor: Transactor[F[Throwable, ?]]
    ) extends SQL[F] {
      override def execute[A](queryName: String)(conn: ConnectionIO[A]): F[QueryFailure, A] = {
        transactor.trans
          .apply(conn)
          .leftMap(QueryFailure(queryName, _))
      }
    }
  }

  object Postgres {

    def resource[F[_]](
      cfg: PostgresCfg,
      blocker: Blocker,
      async: Async[F],
      shift: ContextShift[F],
      @unused pgIntegrationCheck: PgIntegrationCheck,
      portCfg: PostgresPortCfg,
    ): Resource[F, HikariTransactor[F]] = {
      HikariTransactor
        .newHikariTransactor(
          driverClassName = cfg.jdbcDriver,
          url             = portCfg.substitute(cfg.url),
          user            = cfg.user,
          pass            = cfg.password,
          connectEC       = blocker.blockingContext,
          blocker         = blocker,
        )(async, shift)
    }

    final case class PostgresCfg(
      jdbcDriver: String,
      url: String,
      user: String,
      password: String,
    )

    final case class PostgresPortCfg(
      host: String,
      port: Int,
    ) {
      def substitute(s: String): String = {
        s.replace("{host}", host).replace("{port}", port.toString)
      }
    }

    final class PgIntegrationCheck(
      portCheck: PortCheck,
      cfg: PostgresCfg,
      portCfg: PostgresPortCfg,
    ) extends IntegrationCheck {
      override def resourcesAvailable(): ResourceCheck = {
        val str = portCfg.substitute(cfg.url.stripPrefix("jdbc:"))
        val uri = URI.create(str)

        portCheck.checkUri(uri, portCfg.port, s"Couldn't connect to postgres at uri=$uri defaultPort=${portCfg.port}")
      }
    }

  }

  import cats.syntax.functor._
  import doobie.postgres.implicits._
  import doobie.syntax.string._

  object Ladder {

    final class Postgres[F[+_, +_]: BIOMonad](
      sql: SQL[F],
      log: LogBIO[F],
    ) extends DIResource.MakePair[F[Throwable, ?], Ladder[F]](
        acquire = for {
          _ <- log.info("Creating Ladder table")
          _ <- sql.execute("ladder-ddl") {
            sql"""create table if not exists ladder (
                 | user_id uuid not null,
                 | score bigint not null,
                 | primary key (user_id)
                 |) without oids
                 |""".stripMargin.update.run.void
          }
          res = new Ladder[F] {

            override def submitScore(userId: UserId, score: Score): F[QueryFailure, Unit] =
              sql.execute("submit-score") {
                sql"""insert into ladder (user_id, score) values ($userId, $score)
                     |on conflict (user_id) do update set
                     |  score = excluded.score
                     |""".stripMargin.update.run.void
              }

            override val getScores: F[QueryFailure, List[(UserId, Score)]] =
              sql.execute("get-leaderboard") {
                sql"""select user_id, score from ladder order by score DESC
                     |""".stripMargin.query[(UserId, Score)].to[List]
              }
          }
        } yield res -> F.unit
      )
  }

  object Profiles {

    final class Postgres[F[+_, +_]: BIOApplicative](
      sql: SQL[F],
      log: LogBIO[F],
    ) extends DIResource.Self[F[Throwable, ?], Profiles[F]]
      with Profiles[F] {

      override val acquire: F[QueryFailure, Unit] = {
        log.info("Creating Profile table") *>
        sql.execute("ddl-profiles") {
          sql"""create table if not exists profiles (
               |  user_id uuid not null,
               |  name text not null,
               |  description text not null,
               |  primary key (user_id)
               |) without oids
               |""".stripMargin.update.run.void
        }
      }
      override val release: F[QueryFailure, Unit] = F.unit

      override def setProfile(userId: UserId, profile: UserProfile): F[QueryFailure, Unit] = {
        sql.execute("set-profile") {
          sql"""insert into profiles (user_id, name, description)
               |values ($userId, ${profile.name}, ${profile.description})
               |on conflict (user_id) do update set
               |  name = excluded.name,
               |  description = excluded.description
               |""".stripMargin.update.run.void
        }
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

  trait HttpApi[F[_, _]] {
    def http: HttpRoutes[F[Throwable, ?]]
  }

  import io.circe.syntax._
  import org.http4s.circe._
  import org.http4s.syntax.kleisli._

  object HttpApi {

    final class Impl[F[+_, +_]: BIO](
      dsl: Http4sDsl[F[Throwable, ?]],
      ladder: Ladder[F],
      profiles: Profiles[F],
      ranks: Ranks[F],
    ) extends HttpApi[F] {

      import dsl._

      override def http: HttpRoutes[F[Throwable, ?]] = {
        HttpRoutes.of[F[Throwable, ?]] {
          case GET -> Root / "ladder" =>
            Ok(ladder.getScores.map(_.asJson))

          case POST -> Root / "ladder" / UUIDVar(userId) / LongVar(score) =>
            Ok(ladder.submitScore(userId, score))

          case GET -> Root / "profile" / UUIDVar(userId) =>
            Ok(ranks.getRank(userId).map(_.asJson))

          case rq @ POST -> Root / "profile" / UUIDVar(userId) =>
            Ok(for {
              profile <- rq.decodeJson[UserProfile]
              _       <- profiles.setProfile(userId, profile)
            } yield ())
        }
      }
    }

  }

  /** Example session:
    *
    * {{{
    * curl -X POST http://localhost:8080/ladder/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4/100
    * curl -X POST http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4 -d '{"name": "Kai", "description": "S C A L A"}'
    * curl -X GET http://localhost:8080/profile/50753a00-5e2e-4a2f-94b0-e6721b0a3cc4
    * curl -X GET http://localhost:8080/ladder
    * }}}
    */
  final class LivecodeRole[F[+_, +_]](
    httpApi: HttpApi[F],
  )(implicit
    concurrentEffect2: ConcurrentEffect2[F],
    timer2: Timer2[F],
  ) extends RoleService[F[Throwable, ?]] {
    override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): DIResourceBase[F[Throwable, ?], Unit] = {
      DIResource.fromCats {
        BlazeServerBuilder[F[Throwable, ?]]
          .withHttpApp(httpApi.http.orNotFound)
          .bindLocal(8080)
          .resource
      }.void
    }
  }

  object LivecodeRole extends RoleDescriptor {
    val id = "livecode"
  }

}
