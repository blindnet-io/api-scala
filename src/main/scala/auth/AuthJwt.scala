package io.blindnet.backend
package auth

import cats.data.Kleisli
import cats.effect.IO
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
  def asUser: IO[UserAuthJwt] = if isInstanceOf[UserAuthJwt] then IO.pure(asInstanceOf[UserAuthJwt]) else IO.raiseError(Exception("Wrong JWT type"))
  def asTempUser: IO[TempUserAuthJwt] = if isInstanceOf[TempUserAuthJwt] then IO.pure(asInstanceOf[TempUserAuthJwt]) else IO.raiseError(Exception("Wrong JWT type"))
}

case class UserAuthJwt(appId: String, userId: String, groupId: String) extends AuthJwt
implicit val dUserAuthJwt: Decoder[UserAuthJwt] = Decoder.forProduct3("app", "uid", "gid")(UserAuthJwt.apply)

case class TempUserAuthJwt(appId: String, groupId: Option[String], tokenId: String, userIds: Seq[String]) extends AuthJwt
implicit val dTempUserAuthJwt: Decoder[TempUserAuthJwt] = Decoder.forProduct4("app", "gid", "tid", "uids")(TempUserAuthJwt.apply)

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
            case "jwt" => verifyToken[UserAuthJwt](token)
            case "tjwt" => verifyToken[TempUserAuthJwt](token)
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

  def verifyTokenWithKey(token: String, key: String): IO[UserAuthJwt] =
    JwtCirce.decodeJson(token, key, Seq(JwtAlgorithm.Ed25519), JwtOptions(signature = false, expiration = false, notBefore = false /* TODO */)) match {
      case Failure(ex) => IO.raiseError(Exception("Invalid JWT", ex))
      case Success(value) =>
        value.as[UserAuthJwt] match {
          case Left(ex) => IO.raiseError(Exception("Invalid JWT", ex))
          case Right(value) => IO.pure(value)
        }
    }

  def verifySignatureWithKey(data: String, signature: String, key: String): IO[Unit] =
    if JwtUtils.verify(data.getBytes, Base64.getDecoder.decode(signature), parseKey(key), JwtAlgorithm.Ed25519) then IO.unit
    else IO.raiseError(Exception("Invalid JWT"))

  private def parseKey(raw: String): PublicKey =
    val kf = KeyFactory.getInstance("Ed25519")
    val pubKeyInfo = new SubjectPublicKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), Base64.getDecoder.decode(raw))
    kf.generatePublic(new X509EncodedKeySpec(pubKeyInfo.getEncoded))
}
