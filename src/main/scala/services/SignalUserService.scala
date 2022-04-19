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

class SignalUserService(userRepo: UserRepository[IO], deviceRepo: UserDeviceRepository[IO], otKeyRepo: OneTimeKeyRepository[IO]) {
  def authedRoutes: AuthedRoutes[AuthJwt, IO] = AuthedRoutes.of[AuthJwt, IO] {
    // FR-UM01 Create User
    case req @ POST -> Root / "signal" / "users" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUserNoCheck
        payload <- req.req.as[CreateSignalUserPayload]
        rawJwt <- AuthJwtUtils.getRawToken(req.req)
        _ <- AuthJwtUtils.verifySignatureWithKey(rawJwt, payload.signedJwt, payload.userSigningPublicKey)
        existingUser <- userRepo.findById(uJwt.appId, uJwt.userId)
        _ <- if existingUser.isDefined then IO.unit else userRepo.insert(User(uJwt.appId, uJwt.userId, uJwt.groupId))
        existingDevice <- deviceRepo.findById(uJwt.appId, uJwt.userId, payload.deviceID)
        ret <- if existingDevice.isDefined then BadRequest() else for {
          _ <- deviceRepo.insert(UserDevice(
            uJwt.appId, uJwt.userId, payload.deviceID,
            payload.userSigningPublicKey,
            payload.publicIkID, payload.publicIk,
            payload.publicSpkID, payload.publicSpk,
            payload.pkSig,
          ))
          _ <- payload.signalOneTimeKeys.traverse(item => otKeyRepo.insert(OneTimeKey(
            uJwt.appId, uJwt.userId, payload.deviceID,
            item.publicOpkID, item.publicOpk
          )))
          ret <- Ok(uJwt.userId)
        } yield ret
      } yield ret
  }
}

case class CreateSignalUserPayload(
  deviceID: String,
  userSigningPublicKey: String,
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
