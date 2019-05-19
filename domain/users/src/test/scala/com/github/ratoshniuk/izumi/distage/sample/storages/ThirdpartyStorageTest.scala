package com.github.ratoshniuk.izumi.distage.sample.storages

import com.github.ratoshniuk.izumi.distage.sample.RandomSpec
import com.github.ratoshniuk.izumi.distage.sample.env.{UserRandomSpec, ZIOEnvTest}
import org.scalatest.Assertion
import com.github.pshirshov.izumi.functional.bio.BIO._

abstract class ThirdpartyStorageTest extends ZIOEnvTest
  with Assertion
  with RandomSpec
  with UserRandomSpec {

  "Thirdparty storage" must {

    "fetch user" in testZIOEnv {
      externalStorage.fetchUser(1)
    }

    "fail on excceded bound id" in testZIOEnv {
      for {
        fail1 <- externalStorage.fetchUser(-1).redeemPure(_ => None, Some(_))
        fail2 <- externalStorage.fetchUser(Int.MaxValue).redeemPure(_ => None, Some(_))
        _ = assert(fail1.isEmpty && fail2.isEmpty)
      } yield ()
    }
  }

}

final class DummyThirdpartyStorageTest extends ThirdpartyStorageTest {
  override val dummy: Boolean = true
}

final class ProductionThirdpartyStorageTest extends ThirdpartyStorageTest {
  override val dummy: Boolean = false
}
