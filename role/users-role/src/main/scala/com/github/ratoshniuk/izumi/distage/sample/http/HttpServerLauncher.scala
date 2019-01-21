package com.github.ratoshniuk.izumi.distage.sample.http

import java.util.concurrent.atomic.AtomicReference

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.{Directive, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives
import akka.stream.ActorMaterializer
import com.github.pshirshov.izumi.distage.config.annotations.ConfPath
import com.github.pshirshov.izumi.distage.model.definition.Id
import com.github.ratoshniuk.izumi.distage.sample.http.HttpServerLauncher.HttpConfig

import scala.concurrent.{ExecutionContext, Future}

class HttpServerLauncher
(
  routes: Set[RouterSet]
, httpCfg: HttpConfig @ConfPath("http")
)
(implicit as: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext@Id("akka-ec")) extends AutoCloseable {

  private val serverBinding = new AtomicReference[Future[ServerBinding]]()

  def startSync(): Unit = {
    val router = routes.toList.map(_.akkaRouter) match {
      case Nil => RouteDirectives.reject
      case head :: tail => tail.fold(head)(_ ~ _)
    }
    val bindingFuture = Http().bindAndHandle(router, httpCfg.host, httpCfg.port)
    serverBinding.set(bindingFuture)
  }

  override def close(): Unit = {
    serverBinding.get()
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => as.terminate()) // and shutdown when don
  }
}

object HttpServerLauncher {
  case class HttpConfig(host: String, port: Int)
}
