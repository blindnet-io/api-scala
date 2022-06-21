package io.blindnet.backend
package services

import auth.*
import errors.*
import models.*
import objects.*

import cats.data.*
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

class SignalUserService(userRepo: UserRepository[IO], deviceRepo: UserDeviceRepository[IO], otKeyRepo: OneTimeKeyRepository[IO]) {
  def createUser(jwt: AuthJwt)(rawAuthHeader: Option[String], payload: CreateSignalUserPayload): IO[String] =
    for {
      uJwt: UserJwt <- jwt.asUserNoCheck
      rawJwt <- IO.fromEither(AuthJwtUtils.extractTokenFromHeader(rawAuthHeader).left.map(e => AuthException(e)))
      _ <- AuthJwtUtils.verifySignatureWithKey(rawJwt, payload.signedJwt, payload.publicIk)
      _ <- AuthJwtUtils.verifyB64SignatureWithKey(payload.publicSpk, payload.pkSig, payload.publicIk)
      existingUser <- userRepo.findById(uJwt.appId, uJwt.userId)
      _ <- if existingUser.isDefined then IO.unit else userRepo.insert(User(uJwt.appId, uJwt.userId, uJwt.groupId))
      _ <- deviceRepo.findById(uJwt.appId, uJwt.userId, payload.deviceID).thenBadRequest("User already exists")
      _ <- for {
        _ <- deviceRepo.insert(UserDevice(
          uJwt.appId, uJwt.userId, payload.deviceID,
          payload.publicIkID, payload.publicIk,
          payload.publicSpkID, payload.publicSpk,
          payload.pkSig,
        ))
        _ <- insertOneTimeKeys(uJwt.appId, uJwt.userId, payload.deviceID, payload.signalOneTimeKeys)
      } yield ()
    } yield uJwt.userId

  def updateUser(jwt: AuthJwt)(payload: UpdateSignalUserPayload): IO[Boolean] =
    for {
      uJwt: UserJwt <- jwt.asUser
      hasSpk = payload.publicSpkID.isDefined && payload.publicSpk.isDefined && payload.pkSig.isEmpty
      hasOtk = payload.signalOneTimeKeys.isDefined
      _ <- if hasSpk || hasOtk then IO.unit else IO.raiseError(BadRequestException("Spk or at least one Otk required"))
      _ <- if !hasSpk then IO.unit else
        deviceRepo.updateSpkById(uJwt.appId, uJwt.userId, payload.deviceID,
          payload.publicSpkID.get, payload.publicSpk.get, payload.pkSig.get)
      _ <- if !hasOtk then IO.unit else
        otKeyRepo.deleteAllByDevice(uJwt.appId, uJwt.userId, payload.deviceID)
          .flatMap(_ => insertOneTimeKeys(uJwt.appId, uJwt.userId, payload.deviceID, payload.signalOneTimeKeys.get))
    } yield true

  def getUserKeys(jwt: AuthJwt)(userId: String, deviceIds: List[String]): IO[List[UserKeysResponseItem]] =
    for {
      uJwt: UserJwt <- jwt.asUser
      devices <- NonEmptyList.fromFoldable(deviceIds) match
        case Some(nel) => deviceRepo.findAllByUserAndIds(uJwt.appId, userId, nel)
        case None => deviceRepo.findAllByUser(uJwt.appId, userId)
      items <- if devices.isEmpty then IO.raiseError(NotFoundException()) else
        devices.traverse(device =>
          otKeyRepo.findByDevice(uJwt.appId, userId, device.id)
            .flatTap(opt => opt match
              case Some(otKey) => otKeyRepo.deleteById(otKey.appId, otKey.userId, otKey.deviceId, otKey.id)
              case None => IO.unit)
            .map(otKey => UserKeysResponseItem(device, otKey)))
    } yield items

  def getUserDevices(jwt: AuthJwt)(userId: String): IO[List[UserDevicesResponseItem]] =
    for {
      uJwt: UserJwt <- jwt.asUser
      devices <- deviceRepo.findAllByUser(uJwt.appId, userId)
      _ <- devices.nonEmpty.orNotFound
    } yield devices.map(UserDevicesResponseItem.apply)

  private def insertOneTimeKeys(appId: String, userId: String, deviceId: String, list: List[OneTimeKeyPayload]): IO[Unit] =
    if list.isEmpty
    then IO.raiseError(BadRequestException("Empty OTK array"))
    else otKeyRepo.insertMany(list.map(item => OneTimeKey(
      appId, userId, deviceId, item.publicOpkID, item.publicOpk
    )))
}
