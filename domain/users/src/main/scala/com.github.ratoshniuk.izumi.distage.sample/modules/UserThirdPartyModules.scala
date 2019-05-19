package com.github.ratoshniuk.izumi.distage.sample.modules

import com.github.pshirshov.izumi.distage.model.definition.ModuleDef
import com.github.pshirshov.izumi.distage.model.definition.StandardAxis.Repo
import com.github.ratoshniuk.izumi.distage.sample.users.services.UserThirdParty
import com.github.ratoshniuk.izumi.distage.sample.users.services.dummy.DummyUserThirdParty
import com.github.ratoshniuk.izumi.distage.sample.users.services.production.ProductionUserThirdparty
import distage.TagKK

object UserThirdPartyModules {

  class UserThirdPartyDummyBase[F[+ _, + _]: TagKK] extends ModuleDef {
    tag(Repo.Dummy)

    make[UserThirdParty[F]].from[DummyUserThirdParty[F]]
  }

  class UserThirdPartyProductionBase[F[+ _, + _]: TagKK] extends ModuleDef {
    tag(Repo.Prod)

    make[UserThirdParty[F]].from[ProductionUserThirdparty[F]]
  }
}
