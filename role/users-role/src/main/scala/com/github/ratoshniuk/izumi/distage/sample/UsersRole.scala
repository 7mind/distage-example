package com.github.ratoshniuk.izumi.distage.sample

import com.github.pshirshov.izumi.distage.model.definition.DIResource
import com.github.pshirshov.izumi.distage.roles.model.{RoleDescriptor, RoleService}
import com.github.pshirshov.izumi.fundamentals.platform.cli.model.raw.RawEntrypointParams
import com.github.pshirshov.izumi.fundamentals.platform.language.Quirks._
import com.github.ratoshniuk.izumi.distage.sample.http.HttpServerLauncher
import logstage.LogBIO

class UsersRole[F[+ _, + _]]
(
  http: HttpServerLauncher.StartedServer
, log: LogBIO[F]
) extends RoleService[F[Throwable, ?]] {

  // force resource start-up
  http.discard()

  override def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]): DIResource[F[Throwable, ?], Unit] = {
    DIResource.make(
      acquire = log.info("Entrypoint reached: users role")
    )(release = _ =>
      log.info("Exit reached: users role")
    )
  }
}

object UsersRole extends RoleDescriptor {
  override final val id = "users"
}
