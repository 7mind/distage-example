package com.github.ratoshniuk.izumi.distage.sample.http

import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import com.github.pshirshov.izumi.functional.bio.BIO._
import com.github.pshirshov.izumi.functional.bio.{BIO, BIORunner}
import com.github.ratoshniuk.izumi.distage.sample.http.RouterSet.ResponseData
import com.github.ratoshniuk.izumi.distage.sample.http.RouterSet.ResponseData._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}

abstract class RouterSet[F[+ _, + _] : BIO : BIORunner] {
  def akkaRouter: server.Route


  implicit final class BIOHttpOps[E, Out](bio: F[E, Out]) {

    def asResponse[Err2, Out2](e: E => ResponseData[Err2], out: Out => ResponseData[Out2])
                              (implicit err2dec: Encoder[Err2]
                               , out2dec: Encoder[Out2]): StandardRoute = {
      val response = BIORunner[F].unsafeRunSyncAsEither(bio.leftMap(e).map(out)) match {
        case com.github.pshirshov.izumi.functional.bio.BIOExit.Success(value) =>
          value.code -> value.data.asJson
        case com.github.pshirshov.izumi.functional.bio.BIOExit.Error(exception) =>
          exception.code -> exception.data.asJson
        case com.github.pshirshov.izumi.functional.bio.BIOExit.Termination(value, _) =>
          val resp = internalF(s"Enexpected internal occured. reason: ${value.getMessage}")
          resp.code -> resp.data.asJson
      }
      complete(response)
    }
  }


}

object RouterSet {

  case class ResponseData[T: Decoder](data: T, code: Int)

  object ResponseData {
    def apply[T](f: (Int, T))(implicit decoder: Decoder[T]): ResponseData[T] = new ResponseData(f._2, f._1)

    def success[T](data: T)(implicit decoder: Decoder[T]) = ResponseData(data, 200)

    case class RestException(message: String)

    def internalF(msg: String) = ResponseData(RestException(msg), 500)

    def notFoundF(msg: String) = RouterSet.ResponseData(RestException(msg), 404)

    def rejectedF(msg: String) = RouterSet.ResponseData(RestException(msg), 403)
  }

}

