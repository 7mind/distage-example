package com.github.ratoshniuk.izumi.distage.sample.services

import com.github.pshirshov.izumi.distage.plugins.PluginDef
import com.github.pshirshov.izumi.functional.bio.BIO._
import com.github.ratoshniuk.izumi.distage.sample.{Models, RandomSpec}
import com.github.ratoshniuk.izumi.distage.sample.env.{UserPersistenceHandle, UserRandomSpec, UserServiceHandle, ZIOEnvTest}
import com.github.ratoshniuk.izumi.distage.sample.users.services.production.PostgresDataSource.PostgresCfg
import org.scalatest.Assertion
import scalaz.zio.ZIO

import scala.concurrent.duration._

class PGPlugin extends PluginDef {
  make[PostgresCfg].from {
    PostgresCfg(
      jdbcDriver = "org.postgresql.Driver"
    , url = "jdbc:postgresql://localhost/distage"
    , user = "distage"
    , password = "distage"
    , defTimeout = 20.seconds
    )
  }
}
abstract class UserServiceTest extends ZIOEnvTest
  with Assertion
  with RandomSpec with UserRandomSpec {

  "internal storage" must {

    "fetch correctly" in testZIOEnv {
      val email = random[Email].get
      for {
        res1 <- userService.retrieve(email).redeemPure(_ => None, Some(_))
        _ = assert(res1.isEmpty)
        _ <- userService.upsert(1, email)
        resFromThirdparty <- userService.retrieve(email).redeemPure(_ => None, Some(_))
        _ = assert(resFromThirdparty.isDefined)
        resFromDb <- storage.get(email).redeemPure(_ => None, Some(_))
        _ = assert(resFromThirdparty == resFromDb)
      } yield ()
    }

    "delete correctly" in testZIOEnv {
      val email = random[Email].get

      // show the type of R
      val testCase: ZIO[UserPersistenceHandle with UserServiceHandle, Models.CommonFailure, Unit] =
        for {
          _ <- userService.upsert(1, email)
          resFromThirdparty <- userService.retrieve(email).redeemPure(_ => None, Some(_))
          _ = assert(resFromThirdparty.isDefined)
          _ <- userService.delete(email)
          res2 <- userService.retrieve(email).redeemPure(_ => None, Some(_))
          resFromDb <- storage.get(email).redeemPure(_ => None, Some(_))
          _ = assert(res2 == resFromDb)
        } yield ()

      testCase
    }
  }


}

final class DummyUserServiceTest extends UserServiceTest {
  override val dummy: Boolean = true
}

final class ProductionUserServiceTest extends UserServiceTest {
  override val dummy: Boolean = false
}
