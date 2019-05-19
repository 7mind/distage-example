package com.github.ratoshniuk.izumi.distage.sample.plugins

import com.github.pshirshov.izumi.distage.plugins.PluginDef
import com.github.ratoshniuk.izumi.distage.sample.UsersRole
import com.github.ratoshniuk.izumi.distage.sample.http.routes.UserServiceRest
import com.github.ratoshniuk.izumi.distage.sample.http.{HttpServerLauncher, RouterSet}
import scalaz.zio.IO

class UserRolePlugin extends PluginDef {
  make[UsersRole[IO]]
}

class HttpIOPlugin extends PluginDef {
  many[RouterSet[IO]]
      .add[UserServiceRest[IO]]
  make[HttpServerLauncher.StartedServer].fromResource[HttpServerLauncher[IO]]
}
