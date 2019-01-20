package com.github.ratoshniuk.izumi.distage.sample.users.services

import com.github.ratoshniuk.izumi.distage.sample.Models.CommonFailure
import com.github.ratoshniuk.izumi.distage.sample.users.services.models.{Email, User}

trait UserPersistence[F[+_, +_]] {
  def upsert(user: User): F[CommonFailure, Unit]
  def remove(userId: Email) : F[CommonFailure, Unit]
  def get(userId: Email) : F[CommonFailure, User]
}
