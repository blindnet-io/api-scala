package io.blindnet.backend
package auth

import cats.effect.*
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.x509.{AlgorithmIdentifier, SubjectPublicKeyInfo}
import org.http4s.*
import org.http4s.headers.*
import org.typelevel.ci.*
import pdi.jwt.*

import java.security.{KeyFactory, PublicKey}
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object AuthJwtUtils {
  def getRawToken(req: Request[IO]): IO[String] =
    req.headers.get[Authorization] match {
      case Some(header) =>
        header.credentials match {
          case Credentials.Token(authScheme, token) =>
            if authScheme.equals(ci"Bearer") then IO.pure(token)
            else IO.raiseError(AuthException("Invalid authorization header"))
          case _ => IO.raiseError(AuthException("Invalid authorization header"))
        }
      case None => IO.raiseError(AuthException("Missing or invalid authorization header"))
    }

  def verifySignatureWithKey(data: String, signature: String, key: String): IO[Unit] =
    if JwtUtils.verify(data.getBytes, Base64.getDecoder.decode(signature), parseKey(key), JwtAlgorithm.Ed25519) then IO.unit
    else IO.raiseError(AuthException("Bad signature"))

  def verifyB64SignatureWithKey(data: String, signature: String, key: String): IO[Unit] =
    if JwtUtils.verify(Base64.getDecoder.decode(data), Base64.getDecoder.decode(signature), parseKey(key), JwtAlgorithm.Ed25519) then IO.unit
    else IO.raiseError(AuthException("Bad signature"))

  def parseKey(raw: String): PublicKey =
    val kf = KeyFactory.getInstance("Ed25519")
    val pubKeyInfo = SubjectPublicKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), Base64.getDecoder.decode(raw))
    kf.generatePublic(X509EncodedKeySpec(pubKeyInfo.getEncoded))
}
