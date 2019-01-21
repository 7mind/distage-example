package com.github.ratoshniuk.izumi.distage.sample

object Models {
  case class CommonFailure private (reason: String, throwable: Option[Throwable])

  object CommonFailure {
    def apply(reason: String): CommonFailure = new CommonFailure(reason, None)
    def apply(reason: String, throwable: Throwable): CommonFailure = new CommonFailure(reason, Some(throwable))
  }
}

