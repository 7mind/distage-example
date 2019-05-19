package com.github.ratoshniuk.izumi.distage.sample.users.services

import io.circe.Decoder

object models {

  type Email = String
  final case class User(email: Email, data: UserData)

  final case class UserData(id: Int, firstName: String, secondName: String) {
    def toUser(email: Email) : User = User(email, this)
  }

  object UserData {
    implicit val decoder: Decoder[UserData] = {
      Decoder.forProduct3[UserData, Int, String, String]("id", "first_name", "last_name") {
        case (id, fn, sn) => UserData.apply(id, fn, sn)
      }
    }
  }
}
