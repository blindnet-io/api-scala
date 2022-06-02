package io.blindnet.backend
package services

import auth.*
import errors.*
import models.*

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.*
import cats.effect.std.UUIDGen
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

import java.time.Instant
import java.util.{Random, UUID}
import scala.util.Try

class StorageService(storageObjectRepo: StorageObjectRepository[IO]) {
  implicit val uuidGen: UUIDGen[IO] = UUIDGen.fromSync

  def authedRoutes: AuthedRoutes[AuthJwt, IO] = AuthedRoutes.of[AuthJwt, IO] {
    // Initialize Upload
    case req @ POST -> Root / "init-upload" as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        objId <- UUIDGen.randomString
        obj = auJwt match
          case UserJwt(appId, userId, _, _) => StorageObject(appId, objId, Some(userId), None)
          case TempUserJwt(appId, _, tokenId, _) => StorageObject(appId, objId, None, Some(tokenId))
        _ <- storageObjectRepo.insert(obj)
        res <- Ok(objId)
      } yield res
  }
}
