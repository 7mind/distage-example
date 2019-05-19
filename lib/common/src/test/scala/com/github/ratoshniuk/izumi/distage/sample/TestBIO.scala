package com.github.ratoshniuk.izumi.distage.sample

import com.github.pshirshov.izumi.distage.model.definition.StandardAxis.Repo
import com.github.pshirshov.izumi.distage.model.definition.{Axis, AxisBase}
import com.github.pshirshov.izumi.distage.testkit.{DistageBioSpecBIOSyntax, DistagePluginBioSpec}
import com.github.pshirshov.izumi.fundamentals.platform.build.ExposedTestScope
import org.scalatest.Assertion
import scalaz.zio.IO

import scala.util.Random

@ExposedTestScope
abstract class TestBIO extends DistagePluginBioSpec[IO] with DistageBioSpecBIOSyntax[IO] {

  def dummy: Boolean

  override protected def pluginPackages: Seq[String] = {
    Seq("com.github.ratoshniuk.izumi.distage.sample.plugins")
  }

  override protected def memoizePlugins: Boolean = false

  override protected def activation: Map[AxisBase, Axis.AxisValue] = {
    if (dummy)
      Map(Repo -> Repo.Dummy)
    else
      Map(Repo -> Repo.Prod)
  }
}

@ExposedTestScope
trait RandomSpec {
  this: Assertion =>

  trait Random[T] {
    def perform(): T
  }

  implicit def randomEmail: Random[Email] = {
    () => {
      Email(s"${Random.nextString(5)}@${Random.nextString(3)}.com")
    }
  }

  implicit def randomString: Random[String] = {
    () => {
      s"${Random.nextString(5)}"
    }
  }

  implicit def randomInt: Random[Int] = {
    () => {
      Random.nextInt()
    }
  }

  def random[T: Random]: T = implicitly[Random[T]].perform()

  type Email = RandomSpec.Email
  val Email = RandomSpec.Email
}

object RandomSpec {
  final case class Email(get: String) extends AnyVal
}
