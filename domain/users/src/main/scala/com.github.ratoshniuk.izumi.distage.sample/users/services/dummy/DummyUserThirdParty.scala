package com.github.ratoshniuk.izumi.distage.sample.users.services.dummy

import com.github.pshirshov.izumi.functional.bio.BIO
import com.github.ratoshniuk.izumi.distage.sample.Models.CommonFailure
import com.github.ratoshniuk.izumi.distage.sample.users.services.models.UserData
import com.github.ratoshniuk.izumi.distage.sample.users.services.{UserThirdParty, models}

import scala.language.postfixOps

final class DummyUserThirdParty[F[+_, +_]: BIO] extends UserThirdParty[F] {

  private val idsAllowed : Set[Int] = 1 to 12 toSet

  override def fetchUser(userId: Int): F[CommonFailure, models.UserData] = {
    idsAllowed.find(_ == userId).map(UserData(_, "firstName", "secondName"))
      .map(data => BIO[F].now(data))
      .getOrElse(BIO[F].fail(CommonFailure(s"Can't find user by requested id $userId")))
  }
}
