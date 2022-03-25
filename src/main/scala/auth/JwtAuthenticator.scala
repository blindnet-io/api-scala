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

import java.nio.charset.StandardCharsets
import java.security.Signature
import java.util.Base64
import scala.util.{Failure, Success}

class JwtAuthenticator(appRepo: AppRepository[IO]) {
  private val authenticate: Kleisli[IO, Request[IO], Either[String, AuthJwt]] = Kleisli { (req: Request[IO]) =>
    AuthJwtUtils.getRawToken(req).flatMap(processToken)
  }
  val authMiddleware: AuthMiddleware[IO, AuthJwt] = AuthMiddleware(authenticate, Kleisli(req => OptionT.liftF(IO.raiseError(AuthException(req.context.asInstanceOf[String])))))

  def processToken(token: String): IO[Either[String, AuthJwt]] =
    def getAppKey(claim: JwtClaim): IO[Either[String, String]] =
      parse(claim.content) match {
        case Left(failure) => IO.pure(Left("Invalid JWT claim: bad JSON"))
        case Right(json) => json.hcursor.downField("app").as[String] match {
          case Left(failure) => IO.pure(Left("Invalid JWT claim: no app id"))
          case Right(appId) => appRepo.findById(appId).flatMap {
            case Some(app) => IO.pure(Right(app.publicKey))
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

          if JwtUtils.verify(hdBytes, signatureBytes, AuthJwtUtils.parseKey(key), JwtAlgorithm.Ed25519) then
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
            case "jwt" => parseToken[UserJwt](token, claim)
            case "tjwt" => parseToken[TempUserJwt](token, claim)
            case "cjwt" => parseToken[ClientJwt](token, claim)
            case _ => IO.pure(Left("Invalid JWT type"))
          }
        else IO.pure(Left("Invalid JWT header"))
    }
}
