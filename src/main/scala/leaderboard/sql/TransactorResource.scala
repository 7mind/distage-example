package leaderboard.sql

import cats.effect.{Async, Blocker, ContextShift}
import distage.DIResource
import doobie.hikari.HikariTransactor
import izumi.distage.framework.model.IntegrationCheck
import izumi.fundamentals.platform.integration.{PortCheck, ResourceCheck}
import leaderboard.config.{PostgresCfg, PostgresPortCfg}

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
    portCheck.checkPort(portCfg.host, portCfg.port, s"Couldn't connect to postgres at host=${portCfg.host} port=${portCfg.port}")
  }
}
