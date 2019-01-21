package com.github.ratoshniuk.izumi.distage.sample.http

import com.github.pshirshov.izumi.distage.roles.RoleComponent
import com.github.pshirshov.izumi.fundamentals.platform.language.Quirks._

class HttpComponent
(
  server: HttpServerLauncher
) extends RoleComponent {

  server.forget

  override def start(): Unit = {
    server.startSync()
  }

  override def stop(): Unit = {
    server.close()
  }
}
