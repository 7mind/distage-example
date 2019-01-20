package com.github.ratoshniuk.izumi.distage.sample.plugins

import com.github.pshirshov.izumi.distage.plugins.PluginDef
import com.github.pshirshov.izumi.distage.roles.RoleService
import com.github.ratoshniuk.izumi.distage.sample.UsersRole
import com.github.ratoshniuk.izumi.distage.sample.modules.UserDomainModuleBase
import com.github.ratoshniuk.izumi.distage.sample.modules.UserPersistenceModules.UserDummyPersistenceBase
import com.github.ratoshniuk.izumi.distage.sample.modules.UserThirdPartyModules.UserThirdPartyDummyBase
import scalaz.zio.IO

class UserPersistenceDummyIOPlugin extends UserDummyPersistenceBase[IO] with PluginDef

class UserThirdPartyDummyIOPlugin extends UserThirdPartyDummyBase[IO] with PluginDef

class UserDomainIOPlugin extends UserDomainModuleBase[IO] with PluginDef

class UserRolePlugin extends PluginDef {
  tag("users")
  many[RoleService].add[UsersRole[IO]]
}
