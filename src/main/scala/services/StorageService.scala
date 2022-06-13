package io.blindnet.backend
package services

import auth.*
import azure.AzureStorage
import errors.*
import models.*

import cats.data.{EitherT, Kleisli, NonEmptyList, OptionT}
import cats.effect.*
import cats.effect.std.UUIDGen
import cats.implicits.*
import com.azure.storage.common.StorageSharedKeyCredential
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
import java.time.format.DateTimeFormatter
import java.util.{Base64, Random, UUID}
import scala.util.Try

class StorageService(storageObjectRepo: StorageObjectRepository[IO],
                     storageBlockRepo: StorageBlockRepository[IO],
                     docKeyRepo: DocumentKeyRepository[IO]) {
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
        res <- Ok(InitUploadResponse(objId))
      } yield res

    // Get Block Upload URL
    case req @ POST -> Root / "get-upload-block-url" as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        payload <- req.req.as[BlockUploadUrlPayload]
        _ <- (payload.chunkSize <= 4194304).orBadRequest("Invalid chunk size")
        obj <- storageObjectRepo.findById(auJwt.appId, payload.dataId).orNotFound
        _ <- obj.isOwner(auJwt).orForbidden
        block <- UUIDGen.randomString.map(StorageBlock(auJwt.appId, obj.id, _))
        _ <- storageBlockRepo.insert(block)
        signed <- AzureStorage.signBlockUpload(obj.id, block.idB64, payload.chunkSize)
        res <- Ok(BlockUploadUrlResponse(
          signed.authorization, signed.date, block.idB64, signed.url
        ))
      } yield res

    // Finish Upload
    case req @ POST -> Root / "finish-upload" as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        payload <- req.req.as[FinishUploadPayload]
        _ <- payload.blockIds.nonEmpty.orBadRequest("Empty blockIds")
        nel = NonEmptyList.fromListUnsafe(payload.blockIds.distinct)
        obj <- storageObjectRepo.findById(auJwt.appId, payload.dataId).orNotFound
        _ <- obj.isOwner(auJwt).orForbidden
        _ <- storageBlockRepo.countByIds(auJwt.appId, obj.id, nel.map(decodeB64))
          .map(_ == nel.size).flatMap(_.orBadRequest("Bad blockId"))
        _ <- AzureStorage.finishBlockBlob(obj.id, payload.blockIds)
        res <- Ok(true)
      } yield res

    // Get File URL
    case req @ GET -> Root / "get-file-url" / objectId as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        obj <- storageObjectRepo.findById(uJwt.appId, objectId).orNotFound
        _ <- docKeyRepo.findByDocumentAndUser(uJwt.appId, objectId, uJwt.userId).orNotFound
        signed <- AzureStorage.signBlobDownload(obj.id)
        res <- Ok(FileDownloadUrlResponse(
          signed.authorization, signed.date, signed.url
        ))
      } yield res

    // Set Metadata
    case req @ POST -> Root / "metadata" as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        payload <- req.req.as[SetMetadataPayload]
        obj <- storageObjectRepo.findById(auJwt.appId, payload.dataID).orNotFound
        _ <- obj.isOwner(auJwt).orForbidden
        _ <- storageObjectRepo.updateMetadataById(auJwt.appId, obj.id, payload.metadata)
        res <- Ok(true)
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
  
  private def decodeB64(in: String): String = String(Base64.getDecoder.decode(in))
}

case class InitUploadResponse(
  dataId: String,
)

case class BlockUploadUrlPayload(
  dataId: String,
  chunkSize: Int
)

case class BlockUploadUrlResponse(
  authorization: String,
  date: String,
  blockId: String,
  url: String
)

case class FinishUploadPayload(
  dataId: String,
  blockIds: List[String]
)

case class FileDownloadUrlResponse(
  authorization: String,
  date: String,
  url: String
)

case class SetMetadataPayload(
  dataID: String,
  metadata: String
)
