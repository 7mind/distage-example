package com.github.ratoshniuk.izumi.distage.sample

object Launcher {
  def main(args: Array[String]): Unit = {
    val targs = Array("role", "-i", "users", "-c", "application.conf")
    new DistageApp().main(targs ++ args)
  }
}
