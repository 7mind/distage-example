package com.github.ratoshniuk.izumi.distage.sample.env

import com.github.pshirshov.izumi.distage.model.Locator.LocatorRef
import com.github.pshirshov.izumi.distage.model.definition.Binding.SingletonBinding
import com.github.pshirshov.izumi.distage.model.definition.{ImplDef, Module}
import com.github.ratoshniuk.izumi.distage.sample.plugins.TraitConstructor
import com.github.ratoshniuk.izumi.distage.sample.users.services.models.Email
import com.github.ratoshniuk.izumi.distage.sample.users.services.{UserPersistence, UserService, UserThirdParty, models}
import com.github.ratoshniuk.izumi.distage.sample.{Models, TestBIO}
import distage.{DIKey, ModuleBase, Tag}
import scalaz.zio.{IO, ZIO}

import scala.collection.mutable

trait UserServiceHandle {
  def userService: UserService[IO]
}

trait UserPersistenceHandle {
  def internalStorage: UserPersistence[IO]
}

trait UserThirdPartyHandle {
  def externalStorage: UserThirdParty[IO]
}

trait ZIOEnvTest extends TestBIO {

  val userService: UserService[ZIO[UserServiceHandle, +?, +?]] = new UserService[ZIO[UserServiceHandle, +?, +?]] {
    override def upsert(userId: Int, email: Email): ZIO[UserServiceHandle, Models.CommonFailure, Unit] = {
      ZIO.accessM(_.userService.upsert(userId, email))
    }

    override def retrieve(email: Email): ZIO[UserServiceHandle, Models.CommonFailure, models.User] = {
      ZIO.accessM(_.userService.retrieve(email))
    }

    override def delete(email: Email): ZIO[UserServiceHandle, Models.CommonFailure, Unit] = {
      ZIO.accessM(_.userService.delete(email))
    }
  }

  val storage: UserPersistence[ZIO[UserPersistenceHandle, +?, +?]] = new UserPersistence[ZIO[UserPersistenceHandle, +?, +?]] {
    override def upsert(user: models.User): ZIO[UserPersistenceHandle, Models.CommonFailure, Unit] = {
      ZIO.accessM(_.internalStorage.upsert(user))
    }

    override def remove(userId: Email): ZIO[UserPersistenceHandle, Models.CommonFailure, Unit] = {
      ZIO.accessM(_.internalStorage.remove(userId))
    }


    override def get(userId: Email): ZIO[UserPersistenceHandle, Models.CommonFailure, models.User] = {
      ZIO.accessM(_.internalStorage.get(userId))
    }
  }

  val externalStorage: UserThirdParty[ZIO[UserThirdPartyHandle, +?, +?]] = new UserThirdParty[ZIO[UserThirdPartyHandle, +?, +?]] {
    override def fetchUser(userId: Int): ZIO[UserThirdPartyHandle, Models.CommonFailure, models.UserData] = {
      ZIO.accessM(_.externalStorage.fetchUser(userId))
    }
  }

  // FIXME: hack, add to testkit
  val ctors = mutable.HashMap[DIKey, TraitConstructor[_]]()

  def testZIOEnv[R: Tag: TraitConstructor](zio: ZIO[R, _, _]): Unit = {
    val ctor = TraitConstructor[R]
    ctors += (DIKey.get[R] -> ctor)

    dio { r: R => zio.provide(r) }
  }

  override protected def refineBindings(roots: Set[DIKey], primaryModule: ModuleBase): ModuleBase = {
    val paramsModule = Module.make {
      (roots - DIKey.get[LocatorRef])
//        .filterNot(_.tpe.tpe.typeSymbol.isAbstract)
        .map {
          key =>
            ctors.get(key) match {
              case Some(traitConstructor) =>
                val fn = traitConstructor.provider.get
                SingletonBinding(key, ImplDef.ProviderImpl(fn.ret, fn))
              case None =>
                SingletonBinding(key, ImplDef.TypeImpl(key.tpe))
            }
        }
    }

    val res = paramsModule overridenBy primaryModule
    res
  }

}
