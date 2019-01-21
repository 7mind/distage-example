package com.github.ratoshniuk.izumi.distage.sample.users.services.production

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.github.pshirshov.izumi.distage.model.definition.Id
import com.github.pshirshov.izumi.functional.bio.BIO._
import com.github.pshirshov.izumi.functional.bio.{BIO, BIOAsync}
import com.github.ratoshniuk.izumi.distage.sample.Models
import com.github.ratoshniuk.izumi.distage.sample.Models.CommonFailure
import com.github.ratoshniuk.izumi.distage.sample.users.services.models.UserData
import com.github.ratoshniuk.izumi.distage.sample.users.services.{UserThirdParty, models}
import io.circe.Json
import io.circe.parser.parse
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
class ProductionUserThirdparty[F[+ _, + _] : BIO : BIOAsync]
()
(
  implicit as: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext@Id("akka-ec")
)
  extends UserThirdParty[F] {
  override def fetchUser(userId: Int): F[Models.CommonFailure, models.UserData] = {
    val request = HttpRequest.apply(HttpMethods.GET, Uri(s"https://reqres.in/api/users/$userId"))
    val put = Http().singleRequest(request).flatMap {
      response => {
        response.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
          .map(_.utf8String)
          .map(str => BIO[F].fromEither(parse(str)))
      }
    }

    val a = BIOOps.fromFutureF[F, Throwable, Json](put, 20 seconds)
      .flatMap {
        json =>
          BIO[F].fromEither(json.as[UserData])
            .leftMap(f => new Exception(f.getMessage()))
      }

    a.leftMap(thr => CommonFailure(s"Erro while fetching user from REST API. ${thr.getMessage}"))
  }
}

object BIOOps {

  def fromFuture[F[+ _, + _], T](future: Future[T], duration: Duration)(implicit bio: BIOAsync[F]): F[Throwable, T] = {
    BIO[F].syncThrowable(Await.result(future, Duration.Inf))
      .timeoutFail(new Exception("Operation failed with timeout exceeded"))(duration)
  }

  def fromFutureF[F[+ _, + _], E <: Throwable, T](future: Future[F[E, T]], duration: Duration)(implicit bio: BIOAsync[F]): F[Throwable, T] = {
    fromFuture[F, F[E, T]](future, duration).flatten
  }
}
