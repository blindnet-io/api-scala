package io.blindnet.backend
package auth

import cats.data.Kleisli
import cats.effect.IO
import io.circe.*
import io.circe.syntax.*
import org.http4s.{Credentials, Request}
import org.http4s.headers.Authorization
import org.typelevel.ci.*
import pdi.jwt.*
import pdi.jwt.algorithms.JwtUnknownAlgorithm

import scala.util.{Failure, Success}

case class UserAuthJwt(appId: String, userId: String, groupId: String)
implicit val dUserAuthJwt: Decoder[UserAuthJwt] = Decoder.forProduct3("app", "uid", "gid")(UserAuthJwt.apply)

object AuthJwt {
  val authenticate: Kleisli[IO, Request[IO], Either[String, UserAuthJwt]] = Kleisli { (req: Request[IO]) =>
    req.headers.get[Authorization] match {
      case Some(header) =>
        header.credentials match {
          case Credentials.Token(authScheme, token) =>
            if authScheme.equals(ci"Bearer") then processToken(token)
            else IO.pure(Left("Invalid authorization header"))
          case _ => IO.pure(Left("Invalid authorization header"))
        }
      case None => IO.pure(Left("No authorization header"))
    }
  }

  def processToken(token: String): IO[Either[String, UserAuthJwt]] =
    JwtCirce.decodeAll(token, JwtOptions(signature = false, expiration = false, notBefore = false)) match {
      case Failure(exception) => IO.pure(Left("Illegal JWT format"))
      case Success((header, claim, _)) =>
        if header.algorithm.contains(JwtUnknownAlgorithm("EdDSA")) && header.typ.contains("jwt") then verifyToken(token)
        else IO.pure(Left("Invalid JWT header"))
    }

  def verifyToken(token: String): IO[Either[String, UserAuthJwt]] =
    JwtCirce.decodeJson(token, JwtOptions(signature = false, expiration = false, notBefore = false /* TODO */)) match {
      case Failure(exception) => IO.pure(Left("Invalid JWT"))
      case Success(value) =>
        value.as[UserAuthJwt] match {
          case Left(value) => IO.pure(Left("Invalid JWT"))
          case Right(value) => IO.pure(Right(value))
        }
    }

  def verifyTokenWithKey(token: String, key: String): IO[UserAuthJwt] =
    JwtCirce.decodeJson(token, key, Seq(JwtAlgorithm.Ed25519), JwtOptions(signature = false, expiration = false, notBefore = false /* TODO */)) match {
      case Failure(exception) => IO.raiseError(Exception("Invalid JWT"))
      case Success(value) =>
        value.as[UserAuthJwt] match {
          case Left(value) => IO.raiseError(Exception("Invalid JWT"))
          case Right(value) => IO.pure(value)
        }
    }
}