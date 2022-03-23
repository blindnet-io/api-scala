package io.blindnet.backend
package auth

import models.*

import cats.data.Kleisli
import cats.effect.IO
import io.blindnet.backend.models.UserRepository
import io.circe.*
import io.circe.syntax.*
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.x509.{AlgorithmIdentifier, SubjectPublicKeyInfo}
import org.http4s.{Credentials, Request}
import org.http4s.headers.Authorization
import org.typelevel.ci.*
import pdi.jwt.*
import pdi.jwt.algorithms.JwtUnknownAlgorithm

import java.nio.charset.StandardCharsets
import java.security.spec.{EdECPublicKeySpec, NamedParameterSpec, PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PublicKey}
import java.util.Base64
import scala.util.{Failure, Success}

sealed trait AuthJwt {
  val appId: String

  def asAnyUser: IO[AnyUserJwt] = if isInstanceOf[AnyUserJwt] then IO.pure(asInstanceOf[AnyUserJwt]) else IO.raiseError(Exception("Wrong JWT type"))
  def asUser: IO[UserJwt] = if isInstanceOf[UserJwt] then IO.pure(asInstanceOf[UserJwt]) else IO.raiseError(Exception("Wrong JWT type"))
  def asTempUser: IO[TempUserJwt] = if isInstanceOf[TempUserJwt] then IO.pure(asInstanceOf[TempUserJwt]) else IO.raiseError(Exception("Wrong JWT type"))
  def asClient: IO[ClientJwt] = if isInstanceOf[ClientJwt] then IO.pure(asInstanceOf[ClientJwt]) else IO.raiseError(Exception("Wrong JWT type"))
}

sealed trait AnyUserJwt extends AuthJwt {
  /**
   * If applied on a tempuser JWT, checks whether it contains the given user IDs or if its group contains all provided user IDs.
   * On a user JWT, does nothing.
   * @param userIds User IDs to check
   * @return IO of Unit if check succeeded
   * @throws Exception if check failed
   */
  def containsUserIds(userIds: List[String], userRepo: UserRepository[IO]): IO[Unit] = this match {
    case uJwt: UserJwt => IO.unit
    case tuJwt: TempUserJwt =>
      if tuJwt.userIds.containsSlice(userIds) then IO.unit
      else tuJwt.groupId match {
        case Some(groupId) => userRepo.countByIdsOutsideGroup(groupId, userIds).flatMap {
          wrongUsers => if wrongUsers == 0 then IO.unit else IO.raiseError(Exception("Token does not contain user ID"))
        }
        case None => IO.raiseError(Exception("Token does not contain user ID"))
      }
  }
  
  def containsGroup(groupId: String): Boolean = this match {
    case uJwt: UserJwt => true
    case tuJwt: TempUserJwt => tuJwt.groupId.contains(groupId)
  }
}

case class UserJwt(appId: String, userId: String, groupId: String) extends AnyUserJwt
implicit val dUserAuthJwt: Decoder[UserJwt] = Decoder.forProduct3("app", "uid", "gid")(UserJwt.apply)

case class TempUserJwt(appId: String, groupId: Option[String], tokenId: String, userIds: Seq[String]) extends AnyUserJwt
implicit val dTempUserAuthJwt: Decoder[TempUserJwt] = Decoder.forProduct4("app", "gid", "tid", "uids")(TempUserJwt.apply)

case class ClientJwt(appId: String, tokenId: String) extends AuthJwt
implicit val dClientAuthJwt: Decoder[ClientJwt] = Decoder.forProduct2("app", "tid")(ClientJwt.apply)

object AuthJwt {
  val authenticate: Kleisli[IO, Request[IO], Either[String, AuthJwt]] = Kleisli { (req: Request[IO]) =>
    getRawToken(req).flatMap(processToken)
  }

  def getRawToken(req: Request[IO]): IO[String] =
    req.headers.get[Authorization] match {
      case Some(header) =>
        header.credentials match {
          case Credentials.Token(authScheme, token) =>
            if authScheme.equals(ci"Bearer") then IO.pure(token)
            else IO.raiseError(Exception("Invalid authorization header"))
          case _ => IO.raiseError(Exception("Invalid authorization header"))
        }
      case None => IO.raiseError(Exception("No authorization header"))
    }

  def processToken(token: String): IO[Either[String, AuthJwt]] =
    JwtCirce.decodeAll(token, JwtOptions(signature = false, expiration = false, notBefore = false)) match {
      case Failure(exception) => IO.pure(Left("Illegal JWT format"))
      case Success((header, claim, _)) =>
        if header.algorithm.contains(JwtUnknownAlgorithm("EdDSA")) && header.typ.isDefined then
          header.typ.get match {
            case "jwt" => verifyToken[UserJwt](token)
            case "tjwt" => verifyToken[TempUserJwt](token)
            case "cjwt" => verifyToken[ClientJwt](token)
            case _ => IO.pure(Left("Invalid JWT type"))
          }
        else IO.pure(Left("Invalid JWT header"))
    }

  def verifyToken[T](token: String)(implicit dT: Decoder[T]): IO[Either[String, T]] =
    JwtCirce.decodeJson(token, JwtOptions(signature = false, expiration = false, notBefore = false /* TODO */)) match {
      case Failure(ex) => IO.pure(Left("Invalid JWT"))
      case Success(value) =>
        value.as[T] match {
          case Left(ex) => IO.pure(Left("Invalid JWT"))
          case Right(value) => IO.pure(Right(value))
        }
    }

  def verifyTokenWithKey(token: String, key: String): IO[UserJwt] =
    JwtCirce.decodeJson(token, key, Seq(JwtAlgorithm.Ed25519), JwtOptions(signature = false, expiration = false, notBefore = false /* TODO */)) match {
      case Failure(ex) => IO.raiseError(Exception("Invalid JWT", ex))
      case Success(value) =>
        value.as[UserJwt] match {
          case Left(ex) => IO.raiseError(Exception("Invalid JWT", ex))
          case Right(value) => IO.pure(value)
        }
    }

  def verifySignatureWithKey(data: String, signature: String, key: String): IO[Unit] =
    if JwtUtils.verify(data.getBytes, Base64.getDecoder.decode(signature), parseKey(key), JwtAlgorithm.Ed25519) then IO.unit
    else IO.raiseError(Exception("Invalid JWT"))

  def verifyB64SignatureWithKey(data: String, signature: String, key: String): IO[Unit] =
    if JwtUtils.verify(Base64.getDecoder.decode(data), Base64.getDecoder.decode(signature), parseKey(key), JwtAlgorithm.Ed25519) then IO.unit
    else IO.raiseError(Exception("Bad signature"))

  private def parseKey(raw: String): PublicKey =
    val kf = KeyFactory.getInstance("Ed25519")
    val pubKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), Base64.getDecoder.decode(raw))
    kf.generatePublic(new X509EncodedKeySpec(pubKeyInfo.getEncoded))
}
