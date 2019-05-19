package com.github.ratoshniuk.izumi.distage.sample.modules

import com.github.pshirshov.izumi.distage.model.definition.ModuleDef
import com.github.pshirshov.izumi.distage.model.definition.StandardAxis.Repo
import com.github.ratoshniuk.izumi.distage.sample.users.services.UserPersistence
import com.github.ratoshniuk.izumi.distage.sample.users.services.dummy.DummyUserPersistence
import com.github.ratoshniuk.izumi.distage.sample.users.services.production.{PostgresConnector, PostgresDataSource, PostgresUserPersistence}
import distage.{TagK, TagKK}
import doobie.hikari.HikariTransactor

object UserPersistenceModules {

  class UserDummyPersistenceBase[F[+_, +_]: TagKK] extends ModuleDef {
    tag(Repo.Dummy)

    make[UserPersistence[F]].from[DummyUserPersistence[F]]
  }

  class UserProductionPersistenceBase[F[+_, +_]: TagKK](implicit ev: TagK[F[Throwable, ?]]) extends ModuleDef {
    tag(Repo.Prod)

    make[HikariTransactor[F[Throwable, ?]]].fromResource(PostgresDataSource.resource[F[Throwable, ?]] _)
    make[PostgresConnector[F]].from[PostgresConnector.Impl[F]]
    make[UserPersistence[F]].from[PostgresUserPersistence[F]]
  }


}
