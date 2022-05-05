package io.blindnet.backend
package services

import auth.*
import errors.*
import models.*

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
  def authedRoutes: AuthedRoutes[AuthJwt, IO] = AuthedRoutes.of[AuthJwt, IO] {
    // FR-UM01 Create User
    case req @ POST -> Root / "signal" / "users" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUserNoCheck
        payload <- req.req.as[CreateSignalUserPayload]
        rawJwt <- AuthJwtUtils.getRawToken(req.req)
        _ <- AuthJwtUtils.verifySignatureWithKey(rawJwt, payload.signedJwt, payload.publicIk)
        _ <- AuthJwtUtils.verifyB64SignatureWithKey(payload.publicSpk, payload.pkSig, payload.publicIk)
        existingUser <- userRepo.findById(uJwt.appId, uJwt.userId)
        _ <- if existingUser.isDefined then IO.unit else userRepo.insert(User(uJwt.appId, uJwt.userId, uJwt.groupId))
        existingDevice <- deviceRepo.findById(uJwt.appId, uJwt.userId, payload.deviceID)
        ret <- if existingDevice.isDefined then BadRequest() else for {
          _ <- deviceRepo.insert(UserDevice(
            uJwt.appId, uJwt.userId, payload.deviceID,
            payload.publicIkID, payload.publicIk,
            payload.publicSpkID, payload.publicSpk,
            payload.pkSig,
          ))
          _ <- insertOneTimeKeys(uJwt.appId, uJwt.userId, payload.deviceID, payload.signalOneTimeKeys)
          ret <- Ok(uJwt.userId)
        } yield ret
      } yield ret

    // FR-UM02 Update User
    case req @ PUT -> Root / "signal" / "keys" / "me" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[UpdateSignalUserPayload]
        hasSpk = payload.publicSpkID.isDefined && payload.publicSpk.isDefined && payload.pkSig.isEmpty
        hasOtk = payload.signalOneTimeKeys.isDefined
        _ <- if hasSpk || hasOtk then IO.unit else IO.raiseError(BadRequestException("Spk or at least one Otk required"))
        _ <- if !hasSpk then IO.unit else
          deviceRepo.updateSpkById(uJwt.appId, uJwt.userId, payload.deviceID,
            payload.publicSpkID.get, payload.publicSpk.get, payload.pkSig.get)
        _ <- if !hasOtk then IO.unit else
          otKeyRepo.deleteAllByDevice(uJwt.appId, uJwt.userId, payload.deviceID)
            .flatMap(_ => insertOneTimeKeys(uJwt.appId, uJwt.userId, payload.deviceID, payload.signalOneTimeKeys.get))
        ret <- Ok(true)
      } yield ret

    // FR-UM03 Get User Keys
    case req @ GET -> Root / "signal" / "keys" / userId as jwt =>
      val deviceIds = req.req.multiParams.getOrElse("deviceID", Nil)
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
        ret <- Ok(items)
      } yield ret

    // Get User Devices
    case req @ GET -> Root / "signal" / "devices" / userId as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        devices <- deviceRepo.findAllByUser(uJwt.appId, userId)
        ret <- if devices.isEmpty then IO.raiseError(NotFoundException()) else
          Ok(devices.map(UserDevicesResponseItem.apply))
      } yield ret
  }

  private def insertOneTimeKeys(appId: String, userId: String, deviceId: String, list: List[OneTimeKeyPayload]): IO[Unit] =
    otKeyRepo.insertMany(list.map(item => OneTimeKey(
      appId, userId, deviceId, item.publicOpkID, item.publicOpk
    )))
}

case class CreateSignalUserPayload(
  deviceID: String,
  publicIkID: String,
  publicIk: String,
  publicSpkID: String,
  publicSpk: String,
  pkSig: String,
  signedJwt: String,
  signalOneTimeKeys: List[OneTimeKeyPayload],
)

case class OneTimeKeyPayload(
  publicOpkID: String,
  publicOpk: String,
)

case class UpdateSignalUserPayload(
  deviceID: String,
  publicSpkID: Option[String],
  publicSpk: Option[String],
  pkSig: Option[String],
  signalOneTimeKeys: Option[List[OneTimeKeyPayload]],
)

case class UserKeysResponseItem(
  userID: String,
  deviceID: String,
  publicIkID: String,
  publicIk: String,
  publicSpkID: String,
  publicSpk: String,
  pkSig: String,
  publicOpkID: Option[String],
  publicOpk: Option[String],
)
object UserKeysResponseItem {
  def apply(device: UserDevice, otKey: Option[OneTimeKey] = None): UserKeysResponseItem =
    new UserKeysResponseItem(
      device.userId, device.id,
      device.publicIkId, device.publicIk,
      device.publicSpkId, device.publicSpk,
      device.pkSig,
      otKey.map(_.id), otKey.map(_.key)
    )
}

case class UserDevicesResponseItem(
  userID: String,
  deviceID: String,
)
object UserDevicesResponseItem {
  def apply(device: UserDevice): UserDevicesResponseItem =
    new UserDevicesResponseItem(device.userId, device.id)
}
