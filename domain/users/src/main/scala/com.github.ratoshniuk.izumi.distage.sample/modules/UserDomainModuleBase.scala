package com.github.ratoshniuk.izumi.distage.sample.modules

import com.github.pshirshov.izumi.functional.bio.BIO
import com.github.ratoshniuk.izumi.distage.sample.users.services.UserService
import distage.{ModuleDef, TagKK}

class UserDomainModuleBase[F[+_, +_]: BIO: TagKK] extends ModuleDef {
  tag("users")
  make[UserService[F]]
}
