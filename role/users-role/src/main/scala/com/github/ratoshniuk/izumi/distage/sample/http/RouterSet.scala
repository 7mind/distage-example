package com.github.ratoshniuk.izumi.distage.sample.http

import akka.http.scaladsl.server

abstract class RouterSet {
  def akkaRouter : server.Route
}
