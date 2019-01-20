package com.github.ratoshniuk.izumi.distage.sample.users.services

object models {

  type Email = String
  case class User(id: Email, data: UserData)
  case class UserData(id: Int, firstName: String, secondName: String) {
    def toUser(email: Email) : User = User(email, this)
  }
}
