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

class UserService(userRepo: UserRepository[IO], keysRepo: UserKeysRepository[IO]) {
  def authedRoutes: AuthedRoutes[AuthJwt, IO] = AuthedRoutes.of[AuthJwt, IO] {
    // FR-BE01 Create User
    case req @ POST -> Root / "users" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUserNoCheck
        payload <- req.req.as[CreateUserPayload]
        rawJwt <- AuthJwtUtils.getRawToken(req.req)
        _ <- AuthJwtUtils.verifySignatureWithKey(rawJwt, payload.signedJwt, payload.publicSigningKey)
        _ <- AuthJwtUtils.verifyB64SignatureWithKey(payload.publicEncryptionKey, payload.signedPublicEncryptionKey, payload.publicSigningKey)
        existingUser <- userRepo.findById(uJwt.appId, uJwt.userId)
        _ <- if existingUser.isDefined then IO.unit else userRepo.insert(User(uJwt.appId, uJwt.userId, uJwt.groupId))
        existingKey <- keysRepo.findById(uJwt.appId, uJwt.userId)
        ret <- if existingKey.isDefined then BadRequest() else keysRepo.insert(UserKeys(
          uJwt.appId, uJwt.userId,
          payload.publicEncryptionKey, payload.publicSigningKey,
          payload.signedPublicEncryptionKey,
          payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey,
          payload.keyDerivationSalt)
        ).flatMap(_ => Ok(uJwt.userId))
      } yield ret

    // FR-BE02 Get Self Keys
    case req @ GET -> Root / "keys" / "me" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        ret <- keysRepo.findById(uJwt.appId, uJwt.userId).flatMap {
          case Some(k) => Ok(UserKeysResponse(
            userID = k.userId,
            publicEncryptionKey = k.publicEncKey,
            publicSigningKey = k.publicSignKey,
            encryptedPrivateEncryptionKey = k.encPrivateEncKey,
            encryptedPrivateSigningKey = k.encPrivateSignKey,
            keyDerivationSalt = k.keyDerivationSalt,
            signedPublicEncryptionKey = k.signedPublicEncKey,
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
    case req @ POST -> Root / "keys" as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        ret <- req.req.as[UsersPublicKeysPayload].flatMap {
          case GIDUsersPublicKeysPayload(groupID) =>
            if auJwt.containsGroup(groupID)
            then Ok(keysRepo.findAllByGroup(auJwt.appId, groupID).map(users => users.map(UserPublicKeysResponse.apply)))
            else Forbidden()
          case UIDUsersPublicKeysPayload(userIDs) => for {
            _ <- auJwt.containsUserIds(userIDs, userRepo)
            ret <- Ok(findUsersPublicKeys(auJwt.appId, userIDs))
          } yield ret
        }
      } yield ret

    // FR-BE09 Update Private Keys
    case req @ PUT -> Root / "keys" / "me" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[UpdateUserPrivateKeysPayload]
        _ <- payload.keyDerivationSalt match {
          case Some(salt) => keysRepo.updatePrivateKeysAndSalt(uJwt.appId, uJwt.userId, payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey, salt)
          case None => keysRepo.updatePrivateKeys(uJwt.appId, uJwt.userId, payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey)
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
    keysRepo.findById(appId, id).flatMap {
      case Some(u) => IO.pure(UserPublicKeysResponse(u))
      case None => IO.raiseError(NotFoundException("User not found"))
    }

  private def findUsersPublicKeys(appId: String, ids: List[String]): IO[List[UserPublicKeysResponse]] =
    keysRepo.findAllByIds(appId, ids)
      .ensureSize(ids.size, NotFoundException("User not found"))
      .map(_.map(UserPublicKeysResponse.apply))
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
  def apply(keys: UserKeys): UserPublicKeysResponse = UserPublicKeysResponse(keys.userId, keys.publicEncKey, keys.publicSignKey, keys.signedPublicEncKey)
}

sealed trait UsersPublicKeysPayload
case class GIDUsersPublicKeysPayload(groupId: String) extends UsersPublicKeysPayload
case class UIDUsersPublicKeysPayload(userIds: List[String]) extends UsersPublicKeysPayload
