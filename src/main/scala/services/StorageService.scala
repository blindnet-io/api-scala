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

class StorageService(storageObjectRepo: StorageObjectRepository[IO], docKeyRepo: DocumentKeyRepository[IO]) {
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

    // Set Metadata
    case req @ POST -> Root / "metadata" as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        payload <- req.req.as[SetMetadataPayload]
        obj <- storageObjectRepo.findById(auJwt.appId, payload.dataID).orNotFound
        _ <- obj.isOwner(auJwt).orForbidden
        _ <- storageObjectRepo.updateMetadataById(auJwt.appId, obj.id, payload.metadata)
        res <- Ok()
      } yield res

    // Get Metadata
    case req @ GET -> Root / "metadata" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        dataId <- req.req.params.get("dataId").orBadRequest("Missing dataId")
        obj <- storageObjectRepo.findById(uJwt.appId, dataId).orNotFound
        _ <- docKeyRepo.findByDocumentAndUser(uJwt.appId, dataId, uJwt.userId).orNotFound
        res <- Ok(obj.meta)
      } yield res
  }
}

case class SetMetadataPayload(
  dataID: String,
  metadata: String
)
