package com.github.ratoshniuk.izumi.distage.sample.plugins

import com.github.pshirshov.izumi.distage.plugins.PluginDef
import com.github.pshirshov.izumi.distage.roles.RoleService
import com.github.ratoshniuk.izumi.distage.sample.UsersRole
import com.github.ratoshniuk.izumi.distage.sample.http.routes.UserServiceRest
import com.github.ratoshniuk.izumi.distage.sample.http.{HttpComponent, HttpServerLauncher, RouterSet}
import scalaz.zio.IO

class UserRolePlugin extends PluginDef {
  tag("users")
  make[RoleService].named("users").from[UsersRole[IO]]
}

class HttpIOPlugin extends PluginDef {
  tag("users")
  many[RouterSet[IO]]
      .add[UserServiceRest[IO]]
  make[HttpServerLauncher[IO]]
  make[HttpComponent[IO]]
}
