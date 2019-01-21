package com.github.ratoshniuk.izumi.distage.sample.http

import com.github.pshirshov.izumi.distage.roles.RoleComponent
import com.github.pshirshov.izumi.functional.bio.BIO
import com.github.pshirshov.izumi.fundamentals.platform.language.Quirks._

class HttpComponent[F[+_, +_]: BIO]
(
  server: HttpServerLauncher[F]
) extends RoleComponent {

  server.forget

  override def start(): Unit = {
    server.startSync()
  }

  override def stop(): Unit = {
    server.close()
  }
}
