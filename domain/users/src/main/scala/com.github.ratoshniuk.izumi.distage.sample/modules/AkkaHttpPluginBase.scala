package com.github.ratoshniuk.izumi.distage.sample.modules

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.pshirshov.izumi.distage.model.definition.{Id, ModuleDef}

import scala.concurrent.ExecutionContext

class AkkaHttpPluginBase extends ModuleDef {
  tag("users")

  make[ActorSystem].from {
    ActorSystem("distage-sample")
  }

  make[ActorMaterializer].from {
    system: ActorSystem =>
      ActorMaterializer()(system)
  }

  make[ExecutionContext].named("akka-ec").from {
    system: ActorSystem =>
      system.dispatcher
   }
}
