package com.github.ratoshniuk.izumi.distage.sample

import java.util.concurrent.Executors

import com.github.pshirshov.izumi.distage.model.definition.BindingTag
import com.github.pshirshov.izumi.distage.roles.BackendPluginTags
import com.github.pshirshov.izumi.distage.testkit.DistagePluginSpec
import com.github.pshirshov.izumi.functional.bio.{BIO, BIORunner}
import com.github.pshirshov.izumi.fundamentals.platform.build.ExposedTestScope
import com.github.pshirshov.izumi.logstage.api.IzLogger
import com.github.ratoshniuk.izumi.distage.sample.plugins.LoggingZioRunner
import distage.Tag
import org.scalatest.Assertion
import scalaz.zio.IO

import scala.util.Random

@ExposedTestScope
trait TestBIO extends DistagePluginSpec {

  implicit val bio: BIO[IO] = BIO[IO]
  implicit val bioRunner: BIORunner[IO] = {
    val cores = Runtime.getRuntime.availableProcessors.max(2)
    val es = Executors.newFixedThreadPool(cores)
    LoggingZioRunner.apply(es, IzLogger.DebugLogger)
  }

  def testBIO[T: Tag, E, A](f: T => IO[E, A])(implicit bio: BIO[IO], run: BIORunner[IO]): Any = {
    di[T] {
      cxt =>
        val z = bio.leftMap(f(cxt))(err => fail(s"failed running bio. reason: $err"))
        run.unsafeRun(z)
    }
  }


  def dummy: Boolean

  override protected def pluginPackages: Seq[String] = super.pluginPackages ++
    Seq("com.github.ratoshniuk.izumi.distage.sample.plugins")

  override protected def disabledTags: BindingTag.Expressions.Expr = {
    if (dummy) {
      BindingTag.Expressions.all(BackendPluginTags.Production, BackendPluginTags.Storage)
    } else {
      BindingTag.Expressions.any(BackendPluginTags.Test, BackendPluginTags.Dummy)
    }
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
      new Email(s"${Random.nextString(5)}@${Random.nextString(3)}.com")
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

  class Email(val get: String) extends AnyRef

}

