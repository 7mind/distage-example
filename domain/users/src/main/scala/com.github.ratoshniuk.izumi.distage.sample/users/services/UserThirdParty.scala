package com.github.ratoshniuk.izumi.distage.sample.users.services

import com.github.ratoshniuk.izumi.distage.sample.Models.CommonFailure
import com.github.ratoshniuk.izumi.distage.sample.users.services.models.UserData

trait UserThirdParty[F[+_, +_]] {
  def fetchUser(userId: Int) : F[CommonFailure, UserData]
}
