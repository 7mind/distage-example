package livecode.sql

import java.net.URI

import cats.effect.{Async, Blocker, ContextShift, Resource}
import doobie.hikari.HikariTransactor
import izumi.distage.framework.model.IntegrationCheck
import izumi.fundamentals.platform.integration.{PortCheck, ResourceCheck}
import livecode.config.{PostgresCfg, PostgresPortCfg}

import scala.annotation.unused

object Postgres {

  def resource[F[_]](
    cfg: PostgresCfg,
    portCfg: PostgresPortCfg,
    blocker: Blocker,
    async: Async[F],
    shift: ContextShift[F],
    @unused pgIntegrationCheck: PgIntegrationCheck,
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
