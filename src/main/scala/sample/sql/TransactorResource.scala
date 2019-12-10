package sample.sql

import java.net.URI

import cats.effect.{Async, Blocker, ContextShift}
import doobie.hikari.HikariTransactor
import izumi.distage.framework.model.IntegrationCheck
import izumi.distage.model.definition.DIResource
import izumi.fundamentals.platform.integration.{PortCheck, ResourceCheck}
import sample.config.{PostgresCfg, PostgresPortCfg}

final class TransactorResource[F[_]: Async: ContextShift](
  cfg: PostgresCfg,
  portCfg: PostgresPortCfg,
  portCheck: PortCheck,
  blocker: Blocker,
) extends DIResource.OfCats(
    HikariTransactor.newHikariTransactor(
      driverClassName = cfg.jdbcDriver,
      url             = portCfg.substitute(cfg.url),
      user            = cfg.user,
      pass            = cfg.password,
      connectEC       = blocker.blockingContext,
      blocker         = blocker,
    )
  )
  with IntegrationCheck {
  override def resourcesAvailable(): ResourceCheck = {
    val str = portCfg.substitute(cfg.url.stripPrefix("jdbc:"))
    val uri = URI.create(str)

//    portCheck.checkUri(uri, portCfg.port, s"Couldn't connect to postgres at uri=$uri defaultPort=${portCfg.port}")
    ResourceCheck.ResourceUnavailable("cause", None)
  }
}
