package leaderboard.sql

import distage.{Id, Lifecycle}
import doobie.hikari.HikariTransactor
import izumi.distage.model.provisioning.IntegrationCheck
import izumi.fundamentals.platform.integration.{PortCheck, ResourceCheck}
import leaderboard.config.{PostgresCfg, PostgresPortCfg}
import cats.effect.IO

import scala.concurrent.ExecutionContext

final class TransactorResource(
  cfg: PostgresCfg,
  portCfg: PostgresPortCfg,
  portCheck: PortCheck,
  blockingExecutionContext: ExecutionContext @Id("io"),
) extends Lifecycle.OfCats(
    HikariTransactor.newHikariTransactor[IO](
      driverClassName = cfg.jdbcDriver,
      url             = portCfg.substitute(cfg.url),
      user            = cfg.user,
      pass            = cfg.password,
      connectEC       = blockingExecutionContext,
    )
  )
  with IntegrationCheck[IO] {
  override def resourcesAvailable(): IO[ResourceCheck] = IO {
    portCheck.checkPort(portCfg.host, portCfg.port, s"Couldn't connect to postgres at host=${portCfg.host} port=${portCfg.port}")
  }
}
