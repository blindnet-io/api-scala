package io.blindnet.backend
package auth

import models.*

import cats.data.*
import cats.effect.*
import io.circe.*
import io.circe.parser.*
import org.http4s.*
import org.http4s.server.*
import pdi.jwt.*
import pdi.jwt.algorithms.*
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint

import java.nio.charset.StandardCharsets
import java.security.{PublicKey, Signature}
import java.util.Base64
import scala.util.{Failure, Success, Try}

class JwtAuthenticator(appRepo: AppRepository[IO], userRepo: UserRepository[IO]) {
  val secureEndpoint: PartialServerEndpoint[String, AuthJwt, Unit, String, Unit, Any, IO] =
    endpoint
      .securityIn(auth.bearer[String]())
      .errorOut(plainBody[String])
      .serverSecurityLogic(processToken)

  def processToken(token: String): IO[Either[String, AuthJwt]] =
    def getAppKey(claim: JwtClaim): IO[Either[String, PublicKey]] =
      parse(claim.content) match {
        case Left(failure) => IO.pure(Left("Invalid JWT claim: bad JSON"))
        case Right(json) => json.hcursor.downField("app").as[String] match {
          case Left(failure) => IO.pure(Left("Invalid JWT claim: no app id"))
          case Right(appId) => appRepo.findById(appId).flatMap {
            case Some(app) => AuthJwtUtils.parseKey(app.publicKey) match {
              case Failure(ex) => IO.raiseError(ex)
              case Success(parsedKey) => IO.pure(Right(parsedKey))
            }
            case None => IO.pure(Left("Unknown app"))
          }
        }
      }

    def parseToken[T](token: String, claim: JwtClaim)(implicit dT: Decoder[T]): IO[Either[String, T]] =
      getAppKey(claim).flatMap {
        case Left(error) => IO.pure(Left(error))
        case Right(key) =>
          val spl = token.split("\\.")
          val hd = spl(0) + "." + spl(1)
          val hdBytes = hd.getBytes(StandardCharsets.UTF_8)
          val signatureBytes = Base64.getUrlDecoder.decode(spl(2))

          if
            Try {
              JwtUtils.verify(hdBytes, signatureBytes, key, JwtAlgorithm.Ed25519)
            }.recover(_ => false).get
          then
            JwtCirce.decodeJson(token, JwtOptions(signature = false, expiration = true, notBefore = true)) match {
              case Failure(ex) => IO.pure(Left("Invalid JWT: " + ex.getMessage))
              case Success(value) =>
                value.as[T] match {
                  case Left(ex) => IO.pure(Left("Invalid JWT: invalid claim for type"))
                  case Right(value) => IO.pure(Right(value))
                }
            }
          else IO.pure(Left("Invalid JWT: bad signature"))
      }

    JwtCirce.decodeAll(token, JwtOptions(signature = false, expiration = false, notBefore = false)) match {
      case Failure(exception) => IO.pure(Left("Illegal JWT format"))
      case Success((header, claim, _)) =>
        if header.algorithm.contains(JwtUnknownAlgorithm("EdDSA")) && header.typ.isDefined then
          header.typ.get match {
            case "jwt" => parseToken[UserJwt](token, claim).flatMap(e => e match
              case Left(_) => IO.pure(e)
              case Right(uJwt) => userRepo.findById(uJwt.appId, uJwt.userId).map(o => Right(
                if o.isDefined then UserJwt(uJwt.appId, uJwt.userId, uJwt.groupId, true) else uJwt
              ))
            )
            case "tjwt" => parseToken[TempUserJwt](token, claim)
            case "cjwt" => parseToken[ClientJwt](token, claim)
            case _ => IO.pure(Left("Invalid JWT type"))
          }
        else IO.pure(Left("Invalid JWT header"))
    }
}
