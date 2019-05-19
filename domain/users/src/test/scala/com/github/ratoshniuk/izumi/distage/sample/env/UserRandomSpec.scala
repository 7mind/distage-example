package com.github.ratoshniuk.izumi.distage.sample.env

import com.github.ratoshniuk.izumi.distage.sample.RandomSpec
import com.github.ratoshniuk.izumi.distage.sample.users.services.models
import com.github.ratoshniuk.izumi.distage.sample.users.services.models.{User, UserData}
import org.scalatest.Assertion

trait UserRandomSpec extends RandomSpec {
  this: Assertion =>
  implicit def randomUserData: Random[UserData] = {
    () => {
      UserData(implicitly[Random[Int]].perform(), implicitly[Random[String]].perform(), implicitly[Random[String]].perform())
    }
  }

  implicit def randomUserUser: Random[User] = {
    () => {
      models.User(implicitly[Random[Email]].perform().get, implicitly[Random[UserData]].perform())
    }
  }
}
