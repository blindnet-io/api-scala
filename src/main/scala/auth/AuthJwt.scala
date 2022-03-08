package io.blindnet.backend
package auth

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.Request
import org.http4s.headers.Authorization
import org.typelevel.ci.*
import pdi.jwt.*

sealed trait AuthJwt
case class ClientAuthJwt(appId: String, requestId: String) extends AuthJwt

object AuthJwt {
  val authenticate: Kleisli[IO, Request[IO], Either[Error, AuthJwt]] = Kleisli { (req: Request[IO]) =>
    req.headers.get[Authorization] match {
      case Some(value) => println(value)
      case None => println("no auth")
    }


    IO(Right(ClientAuthJwt("", "")))
  }
}