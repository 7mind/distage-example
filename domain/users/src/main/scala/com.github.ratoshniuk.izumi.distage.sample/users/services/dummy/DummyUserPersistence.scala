package com.github.ratoshniuk.izumi.distage.sample.users.services.dummy

import com.github.pshirshov.izumi.functional.bio.BIO
import com.github.pshirshov.izumi.functional.bio.BIO._
import com.github.ratoshniuk.izumi.distage.sample.Models.CommonFailure
import com.github.ratoshniuk.izumi.distage.sample.users.services.UserPersistence
import com.github.ratoshniuk.izumi.distage.sample.users.services.models.{Email, User}

class DummyUserPersistence[F[+ _, + _] : BIO] extends UserPersistence[F] {

  private val storage = scala.collection.mutable.HashMap.empty[Email, User]

  override def upsert(user: User): F[CommonFailure, Unit] = {
    syncBIO(storage.update(user.email, user))
  }

  override def remove(userId: Email): F[CommonFailure, Unit] = {
    syncBIO(storage.remove(userId))
      .void
  }

  override def get(userId: Email): F[CommonFailure, User] = {
    syncBIO {
      storage.getOrElse(userId, throw new IllegalArgumentException("Can't fetch user by requested id"))
    }
  }

  private def syncBIO[T](f: T): F[CommonFailure, T] = {
    BIO[F]
      .syncThrowable(synchronized(f))
      .leftMap(thr => CommonFailure("", thr))
  }
}
