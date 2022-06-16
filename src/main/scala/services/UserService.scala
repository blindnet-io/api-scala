package io.blindnet.backend
package services

import auth.*
import errors.*
import models.*
import objects.*

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
  def createUser(jwt: AuthJwt)(rawJwt: String, payload: CreateUserPayload): IO[String] =
    for {
      uJwt: UserJwt <- jwt.asUserNoCheck
      _ <- AuthJwtUtils.verifySignatureWithKey(rawJwt, payload.signedJwt, payload.publicSigningKey)
      _ <- AuthJwtUtils.verifyB64SignatureWithKey(payload.publicEncryptionKey, payload.signedPublicEncryptionKey, payload.publicSigningKey)
      existingUser <- userRepo.findById(uJwt.appId, uJwt.userId)
      _ <- if existingUser.isDefined then IO.unit else userRepo.insert(User(uJwt.appId, uJwt.userId, uJwt.groupId))
      _ <- keysRepo.findById(uJwt.appId, uJwt.userId).thenBadRequest("User already exists")
      _ <- keysRepo.insert(UserKeys(
        uJwt.appId, uJwt.userId,
        payload.publicEncryptionKey, payload.publicSigningKey,
        payload.signedPublicEncryptionKey,
        payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey,
        payload.keyDerivationSalt)
      )
    } yield uJwt.userId

  def getSelfKeys(jwt: AuthJwt)(x: Unit): IO[UserKeysResponse] =
    for {
      uJwt: UserJwt <- jwt.asUser
      keys <- keysRepo.findById(uJwt.appId, uJwt.userId).orNotFound
    } yield UserKeysResponse(
      userID = keys.userId,
      publicEncryptionKey = keys.publicEncKey,
      publicSigningKey = keys.publicSignKey,
      encryptedPrivateEncryptionKey = keys.encPrivateEncKey,
      encryptedPrivateSigningKey = keys.encPrivateSignKey,
      keyDerivationSalt = keys.keyDerivationSalt,
      signedPublicEncryptionKey = keys.signedPublicEncKey,
    )

  def getUserPublicKeys(jwt: AuthJwt)(userId: String): IO[UserPublicKeysResponse] =
    for {
      uJwt: UserJwt <- jwt.asUser
      ret <- findUserPublicKeys(uJwt.appId, userId)
    } yield ret

  def getUsersPublicKeys(jwt: AuthJwt)(payload: UsersPublicKeysPayload): IO[List[UserPublicKeysResponse]] =
    for {
      auJwt: AnyUserJwt <- jwt.asAnyUser
      _ <- (payload.groupId.isDefined ^ payload.userIds.isDefined)
        .orBadRequest("either groupId or userIds must be provided")
      ret <- if payload.groupId.isDefined
      then auJwt.containsGroup(payload.groupId.get).orForbidden
          .flatMap(_ => keysRepo.findAllByGroup(auJwt.appId, payload.groupId.get))
          .map(users => users.map(UserPublicKeysResponse.apply))
      else
        auJwt.containsUserIds(payload.userIds.get, userRepo)
          .flatMap(_ => findUsersPublicKeys(auJwt.appId, payload.userIds.get))
    } yield ret

  def updatePrivateKeys(jwt: AuthJwt)(payload: UpdateUserPrivateKeysPayload): IO[Unit] =
    for {
      uJwt: UserJwt <- jwt.asUser
      _ <- payload.keyDerivationSalt match {
        case Some(salt) => keysRepo.updatePrivateKeysAndSalt(uJwt.appId, uJwt.userId, payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey, salt)
        case None => keysRepo.updatePrivateKeys(uJwt.appId, uJwt.userId, payload.encryptedPrivateEncryptionKey, payload.encryptedPrivateSigningKey)
      }
    } yield ()

  def deleteSelfUser(jwt: AuthJwt)(x: Unit): IO[Unit] =
    for {
      uJwt: UserJwt <- jwt.asUser
      _ <- userRepo.delete(uJwt.appId, uJwt.userId)
    } yield ()

  def deleteUser(jwt: AuthJwt)(userId: String): IO[Unit] =
    for {
      cJwt: ClientJwt <- jwt.asClient
      _ <- userRepo.delete(cJwt.appId, userId)
    } yield ()

  def deleteGroup(jwt: AuthJwt)(groupId: String): IO[Unit] =
    for {
      cJwt: ClientJwt <- jwt.asClient
      _ <- userRepo.deleteAllByGroup(cJwt.appId, groupId)
    } yield ()

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


