package io.blindnet.backend
package services

import auth.*
import models.*

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.*
import cats.implicits.*
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
    // FR-BE01 Create User
    // TODO handle group id
    case req @ POST -> Root / "users" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[CreateUserPayload]
        rawJwt <- AuthJwt.getRawToken(req.req)
        _ <- AuthJwt.verifySignatureWithKey(rawJwt, payload.signedJwt, payload.publicSigningKey)
        existing <- userRepo.findById(uJwt.userId)
        ret <- existing match {
          case Some(_) => BadRequest()
          case None => for {
            _ <- AuthJwt.verifySignatureWithKey(payload.publicEncryptionKey, payload.signedPublicEncryptionKey, payload.publicSigningKey)
            _ <- userRepo.insert(User(
              uJwt.appId, uJwt.userId,
              payload.publicEncryptionKey, payload.publicSigningKey,
              payload.signedPublicEncryptionKey,
              payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey,
              payload.keyDerivationSalt)
            )
            ret <- Ok(uJwt.userId)
          } yield ret
        }
      } yield ret

    // FR-BE02 Get Self Keys
    case req @ GET -> Root / "keys" / "me" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        ret <- userRepo.findById(uJwt.userId).flatMap {
          case Some(u) => Ok(UserKeysResponse(
            userID = u.id, // TODO swagger vs FRD
            publicEncryptionKey = u.publicEncKey,
            publicSigningKey = u.publicSignKey,
            encryptedPrivateEncryptionKey = u.encPrivateEncKey,
            encryptedPrivateSigningKey = u.encPrivateSignKey,
            keyDerivationSalt = u.keyDerivationSalt,
            signedPublicEncryptionKey = u.signedPublicEncKey, // TODO swagger vs FRD
          ))
          case None => NotFound()
        }
      } yield ret

    // TODO Does not match FRD nor Swagger but JS SDK expects it
    // TODO Probably matches FR-BE03/4/5 (best 5 but not POST and no req params) but not exactly
    // TODO Handle groups (?)
    // Get Users Public Keys (from a temp user JWT)
    case req @ GET -> Root / "keys" as jwt =>
      for {
        uJwt: TempUserJwt <- jwt.asTempUser
        ret <- Ok(uJwt.userIds.traverse(findUserPublicKeys))
      } yield ret

    // FR-BE03 Get User Public Keys
    // TODO Swagger is probably wrong here about returning an array - this impl matches SDK and FRD
    case req @ GET -> Root / "keys" / userId as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        ret <- Ok(findUserPublicKeys(userId))
      } yield ret

    // FR-BE04 FR-BE05 Get Users Public Keys
    // TODO FRD vs Swagger on allowing no params and using temp token ids instead - here assuming always params (FRD)
    // TODO Handle groups
    case req @ POST -> Root / "keys" as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        ret <- req.req.as[UsersPublicKeysPayload].flatMap {
          case GIDUsersPublicKeysPayload(groupID) => InternalServerError()
          case UIDUsersPublicKeysPayload(userIDs) => for {
            _ <- userIDs.traverse(auJwt.containsUserId)
            ret <- Ok(userIDs.traverse(findUserPublicKeys))
          } yield ret
        }
      } yield ret

    // FR-BE09 Update Private Keys
    case req @ PUT -> Root / "keys" / "me" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[UpdateUserPrivateKeysPayload]
        _ <- payload.keyDerivationSalt match {
          case Some(salt) => userRepo.updatePrivateKeysAndSalt(uJwt.userId, payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey, salt)
          case None => userRepo.updatePrivateKeys(uJwt.userId, payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey)
        }
        ret <- Ok()
      } yield ret

    // FR-BE13 Delete User
    case req @ DELETE -> Root / "users" / userId as jwt =>
      for {
        cJwt: ClientJwt <- jwt.asClient
        _ <- userRepo.delete(userId)
        ret <- Ok()
      } yield ret
  }

  private def findUserPublicKeys(id: String): IO[UserPublicKeysResponse] =
    userRepo.findById(id).flatMap {
      case Some(u) => IO.pure(UserPublicKeysResponse(
        userID = u.id,
        publicEncryptionKey = u.publicEncKey,
        publicSigningKey = u.publicSignKey,
        signedPublicEncryptionKey = u.signedPublicEncKey,
      ))
      case None => IO.raiseError(Exception("User not found"))
    }

  private def authMiddleware = AuthMiddleware(AuthJwt.authenticate, Kleisli(req => OptionT.liftF(Forbidden(req.context))))
  def routes: HttpRoutes[IO] = authMiddleware(authedRoutes)
}

case class CreateUserPayload(
  publicEncryptionKey: String,
  publicSigningKey: String,
  signedJwt: String,
  encryptedPrivateEncryptionKey: String,
  encryptedPrivateSigningKey: String,
  keyDerivationSalt: String,
  signedPublicEncryptionKey: String,
)

case class UpdateUserPrivateKeysPayload(
  encryptedPrivateEncryptionKey: String,
  encryptedPrivateSigningKey: String,
  keyDerivationSalt: Option[String],
)

case class UserKeysResponse(
  userID: String,
  publicEncryptionKey: String,
  publicSigningKey: String,
  encryptedPrivateEncryptionKey: String,
  encryptedPrivateSigningKey: String,
  keyDerivationSalt: String,
  signedPublicEncryptionKey: String,
)

case class UserPublicKeysResponse(
  userID: String,
  publicEncryptionKey: String,
  publicSigningKey: String,
  signedPublicEncryptionKey: String,
)

sealed trait UsersPublicKeysPayload
case class GIDUsersPublicKeysPayload(groupID: String) extends UsersPublicKeysPayload
case class UIDUsersPublicKeysPayload(userIDs: Seq[String]) extends UsersPublicKeysPayload