package com.github.ratoshniuk.izumi.distage.sample.modules

import com.github.ratoshniuk.izumi.distage.sample.users.services.UserService
import distage.{ModuleDef, TagKK}

class UserDomainModuleBase[F[+_, +_]: TagKK] extends ModuleDef {
  make[UserService[F]]
}
