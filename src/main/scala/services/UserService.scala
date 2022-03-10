package io.blindnet.backend
package services

import auth.*
import models.*

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.AuthMiddleware

class UserService(userRepo: UserRepository[IO]) {
  private def authedRoutes = AuthedRoutes.of[AuthJwt, IO] {
    case req @ POST -> Root / "users" as jwt =>
      for {
        uJwt: UserAuthJwt <- jwt.asUser
        payload <- req.req.as[CreateUserPayload]
        rawJwt <- AuthJwt.getRawToken(req.req)
        _ <- AuthJwt.verifySignatureWithKey(rawJwt, payload.signedJwt, payload.publicSigningKey)
        _ <- userRepo.insert(User(
          uJwt.appId, uJwt.userId,
          payload.publicEncryptionKey, payload.publicSigningKey,
          payload.signedPublicEncryptionKey,
          payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey,
          payload.keyDerivationSalt)
        )
        res <- Ok(uJwt.userId)
      } yield res

    case req @ GET -> Root / "keys" / "me" as jwt =>
      for {
        uJwt: UserAuthJwt <- jwt.asUser
        ret <- userRepo.findById(uJwt.userId).flatMap {
          case Some(u) => Ok(UserKeysResponse(
            userID = u.id,
            publicEncryptionKey = u.publicEncKey,
            publicSigningKey = u.publicSignKey,
            encryptedPrivateEncryptionKey = u.encPrivateEncKey,
            encryptedPrivateSigningKey = u.encPrivateSignKey,
            keyDerivationSalt = u.keyDerivationSalt,
            signedPublicEncryptionKey = u.signedPublicEncKey,
          ))
          case None => NotFound()
        }
      } yield ret


    // TODO this is the same as /me just above, refactor that
    case req @ GET -> Root / "keys" as jwt =>
      for {
        uJwt: UserAuthJwt <- jwt.asUser
        ret <- userRepo.findById(uJwt.userId).flatMap {
          case Some(u) => Ok(UserKeysResponse(
            userID = u.id,
            publicEncryptionKey = u.publicEncKey,
            publicSigningKey = u.publicSignKey,
            encryptedPrivateEncryptionKey = u.encPrivateEncKey,
            encryptedPrivateSigningKey = u.encPrivateSignKey,
            keyDerivationSalt = u.keyDerivationSalt,
            signedPublicEncryptionKey = u.signedPublicEncKey,
          ))
          case None => NotFound()
        }
      } yield ret
  }

  private def authMiddleware = AuthMiddleware(AuthJwt.authenticate, Kleisli(req => OptionT.liftF(Forbidden(req.context))))
  def routes: HttpRoutes[IO] = authMiddleware(authedRoutes)
}

case class CreateUserPayload(
  publicEncryptionKey: String,
  publicSigningKey: String,
  signedJwt: String,
  encryptedPrivateEncryptionKey: Option[String],
  encryptedPrivateSigningKey: Option[String],
  keyDerivationSalt: Option[String],
  signedPublicEncryptionKey: Option[String],
)

case class UserKeysResponse(
  userID: String,
  publicEncryptionKey: String,
  publicSigningKey: String,
  encryptedPrivateEncryptionKey: Option[String],
  encryptedPrivateSigningKey: Option[String],
  keyDerivationSalt: Option[String],
  signedPublicEncryptionKey: Option[String],
)
