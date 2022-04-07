package io.blindnet.backend
package services

import auth.*
import errors.*
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
  def authedRoutes: AuthedRoutes[AuthJwt, IO] = AuthedRoutes.of[AuthJwt, IO] {
    // FR-BE01 Create User
    case req @ POST -> Root / "users" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[CreateUserPayload]
        rawJwt <- AuthJwtUtils.getRawToken(req.req)
        _ <- AuthJwtUtils.verifySignatureWithKey(rawJwt, payload.signedJwt, payload.publicSigningKey)
        existing <- userRepo.findById(uJwt.appId, uJwt.userId)
        ret <- existing match {
          case Some(_) => BadRequest()
          case None => for {
            _ <- AuthJwtUtils.verifyB64SignatureWithKey(payload.publicEncryptionKey, payload.signedPublicEncryptionKey, payload.publicSigningKey)
            _ <- userRepo.insert(User(
              uJwt.appId, uJwt.userId, uJwt.groupId,
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
        ret <- userRepo.findById(uJwt.appId, uJwt.userId).flatMap {
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

    // FR-BE03 Get User Public Keys
    case req @ GET -> Root / "keys" / userId as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        ret <- Ok(findUserPublicKeys(uJwt.appId, userId))
      } yield ret

    // FR-BE04 FR-BE05 Get Users Public Keys
    // TODO FRD vs Swagger on allowing no params and using temp token ids instead - here assuming always params (FRD)
    case req @ POST -> Root / "keys" as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        ret <- req.req.as[UsersPublicKeysPayload].flatMap {
          case GIDUsersPublicKeysPayload(groupID) =>
            if auJwt.containsGroup(groupID)
            then Ok(userRepo.findAllByGroup(auJwt.appId, groupID).map(users => users.map(UserPublicKeysResponse.apply)))
            else Forbidden()
          case UIDUsersPublicKeysPayload(userIDs) => for {
            _ <- auJwt.containsUserIds(userIDs, userRepo)
            ret <- Ok(userIDs.traverse(findUserPublicKeys(auJwt.appId, _)))
          } yield ret
        }
      } yield ret

    // FR-BE09 Update Private Keys
    case req @ PUT -> Root / "keys" / "me" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[UpdateUserPrivateKeysPayload]
        _ <- payload.keyDerivationSalt match {
          case Some(salt) => userRepo.updatePrivateKeysAndSalt(uJwt.appId, uJwt.userId, payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey, salt)
          case None => userRepo.updatePrivateKeys(uJwt.appId, uJwt.userId, payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey)
        }
        ret <- Ok()
      } yield ret

    // FR-UM04 Delete Self User
    case req @ DELETE -> Root / "users" / "me" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        _ <- userRepo.delete(uJwt.appId, uJwt.userId)
        ret <- Ok()
      } yield ret

    // FR-BE13 Delete User
    case req @ DELETE -> Root / "users" / userId as jwt =>
      for {
        cJwt: ClientJwt <- jwt.asClient
        _ <- userRepo.delete(cJwt.appId, userId)
        ret <- Ok()
      } yield ret

    // FR-BE14 Delete Group
    case req @ DELETE -> Root / "group" / groupId as jwt =>
      for {
        cJwt: ClientJwt <- jwt.asClient
        _ <- userRepo.deleteAllByGroup(cJwt.appId, groupId)
        ret <- Ok()
      } yield ret
  }

  private def findUserPublicKeys(appId: String, id: String): IO[UserPublicKeysResponse] =
    userRepo.findById(appId, id).flatMap {
      case Some(u) => IO.pure(UserPublicKeysResponse(u))
      case None => IO.raiseError(NotFoundException("User not found"))
    }
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
object UserPublicKeysResponse {
  def apply(user: User): UserPublicKeysResponse = UserPublicKeysResponse(user.id, user.publicEncKey, user.publicSignKey, user.signedPublicEncKey)
}

sealed trait UsersPublicKeysPayload
case class GIDUsersPublicKeysPayload(groupID: String) extends UsersPublicKeysPayload
case class UIDUsersPublicKeysPayload(userIDs: List[String]) extends UsersPublicKeysPayload