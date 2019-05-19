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
import com.github.ratoshniuk.izumi.distage.sample.plugins.BIOFromFuture
import com.github.ratoshniuk.izumi.distage.sample.users.services.models.UserData
import com.github.ratoshniuk.izumi.distage.sample.users.services.{UserThirdParty, models}
import io.circe.parser.parse
import io.circe.{DecodingFailure, Json, ParsingFailure}

import scala.concurrent.ExecutionContext

final class ProductionUserThirdparty[F[+ _, + _]: BIO: BIOAsync: BIOFromFuture]
(implicit
  as: ActorSystem,
  mat: ActorMaterializer,
  ec: ExecutionContext@Id("akka-ec")
)
  extends UserThirdParty[F] {
  override def fetchUser(userId: Int): F[Models.CommonFailure, models.UserData] = {
    val request = HttpRequest(HttpMethods.GET, Uri(s"https://reqres.in/api/users/$userId"))

    val put = BIO[F].syncThrowable {
      Http().singleRequest(request).flatMap {
        response => {
          response.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
            .map(_.utf8String)
            .map(str => {
              BIO[F].fromEither(parse(str))
            })
        }
      }
    }

    BIOFromFuture[F].fromFuture[F[ParsingFailure, Json]](put)
      .flatten
      .leftMap(thr => CommonFailure(s"error while performing http request. ${thr.getMessage}"))
      .flatMap {
        json =>
          val parsed = json.hcursor.downField("data").focus.map(_.as[UserData])
            .getOrElse(Left(DecodingFailure.apply("Error while parsing", Nil)))

          BIO[F].fromEither(parsed)
            .leftMap(f => CommonFailure(s"Error while fetching user from REST API. ${f.getMessage}"))
      }
  }
}
