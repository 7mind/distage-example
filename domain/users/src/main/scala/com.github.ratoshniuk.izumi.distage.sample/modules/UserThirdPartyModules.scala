package com.github.ratoshniuk.izumi.distage.sample.modules

import com.github.pshirshov.izumi.distage.model.definition.ModuleDef
import com.github.pshirshov.izumi.functional.bio.BIO
import com.github.ratoshniuk.izumi.distage.sample.users.services.UserThirdParty
import com.github.ratoshniuk.izumi.distage.sample.users.services.dummy.DummyUserThirdParty
import com.github.ratoshniuk.izumi.distage.sample.users.services.production.ProductionUserThirdparty
import distage.TagKK

object UserThirdPartyModules {

  class UserThirdPartyDummyBase[F[+ _, + _] : BIO : TagKK] extends ModuleDef {
    tag("users", "dummy", "test", "thirdparty")
    make[UserThirdParty[F]].from[DummyUserThirdParty[F]]
  }

  class UserThirdPartyProductionBase[F[+ _, + _] : BIO : TagKK] extends ModuleDef {
    tag("users", "production", "thirdparty")
    make[UserThirdParty[F]].from[ProductionUserThirdparty[F]]
  }
}
