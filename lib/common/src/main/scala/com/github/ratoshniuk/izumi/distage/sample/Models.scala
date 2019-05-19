package com.github.ratoshniuk.izumi.distage.sample

object Models {
  final case class CommonFailure private (reason: String, throwable: Option[Throwable])

  object CommonFailure {
    def apply(reason: String): CommonFailure = new CommonFailure(reason, None)
    def apply(reason: String, throwable: Throwable): CommonFailure = new CommonFailure(reason, Some(throwable))
    def apply(throwable: Throwable): CommonFailure = new CommonFailure(throwable.getMessage, Some(throwable))
  }
}

