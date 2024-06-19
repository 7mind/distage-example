package leaderboard.sql

import cats.effect.{Async, Sync}
import distage.{Id, Lifecycle}
import doobie.hikari.HikariTransactor
import izumi.distage.model.provisioning.IntegrationCheck
import izumi.fundamentals.platform.integration.{PortCheck, ResourceCheck}
import leaderboard.config.{PostgresCfg, PostgresPortCfg}

import scala.concurrent.ExecutionContext

final class TransactorResource[F[_]: Async](
  cfg: PostgresCfg,
  portCfg: PostgresPortCfg,
  portCheck: PortCheck,
  blockingExecutionContext: ExecutionContext @Id("io"),
) extends Lifecycle.OfCats(
    HikariTransactor.newHikariTransactor(
      driverClassName = cfg.jdbcDriver,
      url             = portCfg.substitute(cfg.url),
      user            = cfg.user,
      pass            = cfg.password,
      connectEC       = blockingExecutionContext,
    )
  )
  with IntegrationCheck[F] {
  override def resourcesAvailable(): F[ResourceCheck] = Sync[F].delay {
    portCheck.checkPort(portCfg.host, portCfg.port, s"Couldn't connect to postgres at host=${portCfg.host} port=${portCfg.port}")
  }
}
