package io.blindnet.backend
package auth

import cats.effect.*
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.{AlgorithmIdentifier, SubjectPublicKeyInfo}
import org.http4s.*
import org.http4s.headers.*
import org.typelevel.ci.*
import pdi.jwt.*

import java.security.{KeyFactory, PrivateKey, PublicKey}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.Base64
import scala.util.{Failure, Success, Try}

object AuthJwtUtils {
  def extractTokenFromHeader(header: Option[String]): Either[String, String] =
    header match
      case Some(value) =>
        if value.startsWith("Bearer ")
        then Right(value.substring(7))
        else Left("Invalid authorization header")
      case None => Left("Missing authorization header")

  def verifySignatureWithKey(data: Array[Byte], signature: String, key: String): IO[Unit] =
    decodeBase64(signature).flatMap { signatureBytes =>
      parseKey(key) match {
        case Failure(ex) => IO.raiseError(ex)
        case Success(parsedKey) =>
          if
            Try {
              JwtUtils.verify(data, signatureBytes, parsedKey, JwtAlgorithm.Ed25519)
            }.recover(_ => false).get
          then IO.unit
          else IO.raiseError(AuthException("Bad signature"))
      }
    }

  def verifySignatureWithKey(data: String, signature: String, key: String): IO[Unit] =
    verifySignatureWithKey(data.getBytes, signature, key)

  def verifyB64SignatureWithKey(data: String, signature: String, key: String): IO[Unit] =
    decodeBase64(data).flatMap(verifySignatureWithKey(_, signature, key))

  def parseKey(raw: String): Try[PublicKey] = Try {
    val kf = KeyFactory.getInstance("Ed25519")
    val pubKeyInfo = SubjectPublicKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), Base64.getDecoder.decode(raw))
    kf.generatePublic(X509EncodedKeySpec(pubKeyInfo.getEncoded))
  }

  def parsePrivateKey(raw: String): Try[PrivateKey] = Try {
    val kf = KeyFactory.getInstance("Ed25519")
    val priv = PrivateKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), DEROctetString(Base64.getDecoder.decode(raw)))
    kf.generatePrivate(PKCS8EncodedKeySpec(priv.getEncoded))
  }

  def decodeBase64(data: String): IO[Array[Byte]] = IO.fromTry(Try {
    Base64.getDecoder.decode(data)
  })
}
