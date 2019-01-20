package com.github.ratoshniuk.izumi.distage.sample.modules

import com.github.pshirshov.izumi.distage.model.definition.ModuleDef
import com.github.pshirshov.izumi.functional.bio.BIO
import com.github.ratoshniuk.izumi.distage.sample.users.services.{UserPersistence, UserService}
import com.github.ratoshniuk.izumi.distage.sample.users.services.dummy.DummyUserPersistence
import distage.TagKK

object UserPersistenceModules {

  class UserDummyPersistenceBase[F[+_, +_]: BIO: TagKK] extends ModuleDef {
    tag("users")
//    tag("users", "dummy", "test", "storage")
    make[UserPersistence[F]].from[DummyUserPersistence[F]]
  }

  class UserProductionPersistenceBase[F[+_, +_]: BIO: TagKK] extends ModuleDef {
    tag("users")
//    tag("users", "production", "storage")
    //  make[UserPersistence[F]].from[PostgresUserPeristence[F]
  }


}
