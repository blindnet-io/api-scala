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
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.AuthMiddleware

class UserService(userRepo: UserRepository[IO]) {
  private def authedRoutes = AuthedRoutes.of[UserAuthJwt, IO] {
    case req @ POST -> Root / "users" as jwt =>
      for {
        payload <- req.req.as[CreateUserPayload]
        uJwt <- AuthJwt.verifyTokenWithKey(payload.signedJwt, payload.publicSigningKey)
        _ <- userRepo.insert(User(
          jwt.appId, jwt.userId,
          payload.publicEncryptionKey, payload.publicSigningKey,
          payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey,
          payload.keyDerivationSalt)
        )
        res <- Ok(s"Bonjour ${jwt.userId} ${uJwt.userId}")
      } yield res
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
