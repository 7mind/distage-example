package com.github.ratoshniuk.izumi.distage.sample

import com.github.pshirshov.izumi.distage.roles.{RoleId, RoleService}
import com.github.pshirshov.izumi.functional.bio.{BIO, BIORunner}
import com.github.pshirshov.izumi.logstage.api.IzLogger
import com.github.ratoshniuk.izumi.distage.sample.users.services.UserService

@RoleId("users")
class UsersRole[F[+ _, + _] : BIO: BIORunner]
(
  userService: UserService[F]
  , logger: IzLogger
) extends RoleService {

  override def start(): Unit = {
    logger.info("Entrypoint reached: users role")
  }

  override def stop(): Unit = {
    logger.info("Exit reached: users role")
  }
}
