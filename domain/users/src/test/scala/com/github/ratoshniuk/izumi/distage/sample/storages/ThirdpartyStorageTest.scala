package com.github.ratoshniuk.izumi.distage.sample.storages

import com.github.pshirshov.izumi.functional.bio.BIO._
import com.github.ratoshniuk.izumi.distage.sample.env.UserRandomSpec
import com.github.ratoshniuk.izumi.distage.sample.storages.ThirdpartyStorageTest.Ctx
import com.github.ratoshniuk.izumi.distage.sample.users.services.UserThirdParty
import com.github.ratoshniuk.izumi.distage.sample.{RandomSpec, TestBIO}
import org.scalatest.Assertion
import scalaz.zio.IO

abstract class ThirdpartyStorageTest extends TestBIO
  with Assertion
  with RandomSpec
  with UserRandomSpec {

  "Thirdparty storage" must {

    "fetch user" in dio {
      ctx: Ctx =>
        import ctx._

        storage.fetchUser(1)
    }

    "fail on excceded bound id" in dio {
      ctx: Ctx =>
        import ctx._

        for {
          fail1 <- storage.fetchUser(-1).redeemPure(_ => None, Some(_))
          fail2 <- storage.fetchUser(Int.MaxValue).redeemPure(_ => None, Some(_))
          _ = assert(fail1.isEmpty && fail2.isEmpty)
        } yield ()
    }
  }

}

object ThirdpartyStorageTest {

  case class Ctx(storage: UserThirdParty[IO])

}


final class DummyThirdpartyStorageTest extends ThirdpartyStorageTest {
  override val dummy: Boolean = true
}

final class ProductionThirdpartyStorageTest extends ThirdpartyStorageTest {
  override val dummy: Boolean = false
}
