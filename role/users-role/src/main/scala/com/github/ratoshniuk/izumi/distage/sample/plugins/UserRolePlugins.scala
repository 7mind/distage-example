package com.github.ratoshniuk.izumi.distage.sample.plugins

import com.github.pshirshov.izumi.distage.plugins.PluginDef
import com.github.pshirshov.izumi.distage.roles.RoleService
import com.github.ratoshniuk.izumi.distage.sample.UsersRole
import com.github.ratoshniuk.izumi.distage.sample.http.routes.UserServiceRest
import com.github.ratoshniuk.izumi.distage.sample.http.{HttpComponent, HttpServerLauncher, RouterSet}
import com.github.ratoshniuk.izumi.distage.sample.modules.UserPersistenceModules.{UserDummyPersistenceBase, UserProductionPersistenceBase}
import com.github.ratoshniuk.izumi.distage.sample.modules.UserThirdPartyModules.{UserThirdPartyDummyBase, UserThirdPartyProductionBase}
import com.github.ratoshniuk.izumi.distage.sample.modules.{AkkaHttpPluginBase, UserDomainModuleBase}
import scalaz.zio.IO

class UserPersistenceDummyIOPlugin extends UserDummyPersistenceBase[IO] with PluginDef

class UserPersistenceProductionIOPlugin extends UserProductionPersistenceBase[IO] with PluginDef

class UserThirdPartyDummyIOPlugin extends UserThirdPartyDummyBase[IO] with PluginDef

class UserThirdPartyProductionIOPlugin extends UserThirdPartyProductionBase[IO] with PluginDef

class AkkaHttpPlugin extends AkkaHttpPluginBase with PluginDef

class UserDomainIOPlugin extends UserDomainModuleBase[IO] with PluginDef

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
