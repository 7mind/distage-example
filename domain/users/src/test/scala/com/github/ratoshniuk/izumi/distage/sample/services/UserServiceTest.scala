package com.github.ratoshniuk.izumi.distage.sample.services

import com.github.pshirshov.izumi.distage.plugins.PluginDef
import com.github.pshirshov.izumi.functional.bio.BIO._
import com.github.ratoshniuk.izumi.distage.sample.env.UserRandomSpec
import com.github.ratoshniuk.izumi.distage.sample.services.UserServiceTest.Ctx
import com.github.ratoshniuk.izumi.distage.sample.users.services.production.PostgresCfg
import com.github.ratoshniuk.izumi.distage.sample.users.services.{UserPersistence, UserService}
import com.github.ratoshniuk.izumi.distage.sample.{RandomSpec, TestBIO}
import org.scalatest.Assertion
import scalaz.zio.IO

import scala.concurrent.duration._

class PGPlugin extends PluginDef {
  make[PostgresCfg].from {
    PostgresCfg("org.postgresql.Driver"
      , "jdbc:postgresql://localhost/distage"
      , "distage", "distage", 20.seconds
    )
  }
}
abstract class UserServiceTest extends TestBIO
  with Assertion
  with RandomSpec with UserRandomSpec {

  "internal storage" must {

    "fetch correctly" in testBIO {
      ctx: Ctx =>
        val email = random[Email].get
        for {
          res1 <- ctx.svc.retrieve(email).redeemPure(_ => None, Some(_))
          _ = assert(res1.isEmpty)
          _ <- ctx.svc.upsert(1, email)
          resFromThirdparty <- ctx.svc.retrieve(email).redeemPure(_ => None, Some(_))
          _ = assert(resFromThirdparty.isDefined)
          resFromDb <- ctx.internalStorage.get(email).redeemPure(_ => None, Some(_))
          _ = assert(resFromThirdparty == resFromDb)
        } yield ()
    }

    "delete correctly" in testBIO {
      ctx: Ctx =>
        val email = random[Email].get
        for {
          _ <- ctx.svc.upsert(1, email)
          resFromThirdparty <- ctx.svc.retrieve(email).redeemPure(_ => None, Some(_))
          _ = assert(resFromThirdparty.isDefined)
          _ <- ctx.svc.delete(email)
          res2 <- ctx.svc.retrieve(email).redeemPure(_ => None, Some(_))
          resFromDb <- ctx.internalStorage.get(email).redeemPure(_ => None, Some(_))
          _ = assert(res2 == resFromDb)
        } yield ()
    }
  }


}

object UserServiceTest {

  case class Ctx(svc: UserService[IO], internalStorage: UserPersistence[IO])

}


final class DummyUserServiceTest extends UserServiceTest {
  override val dummy: Boolean = true
}

final class ProdutctionUserServiceTest extends UserServiceTest {
  override val dummy: Boolean = false
}
