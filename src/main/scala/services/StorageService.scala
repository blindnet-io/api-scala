package io.blindnet.backend
package services

import auth.*
import azure.AzureStorage
import errors.*
import models.*
import objects.*

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
  
  def initUpload(jwt: AuthJwt)(x: Unit): IO[InitUploadResponse] =
    for {
      auJwt: AnyUserJwt <- jwt.asAnyUser
      objId <- UUIDGen.randomString
      obj = auJwt match
        case UserJwt(appId, userId, _, _) => StorageObject(appId, objId, Some(userId), None)
        case TempUserJwt(appId, _, tokenId, _) => StorageObject(appId, objId, None, Some(tokenId))
      _ <- storageObjectRepo.insert(obj)
    } yield InitUploadResponse(objId)
  
  def getBlockUploadUrl(jwt: AuthJwt)(payload: BlockUploadUrlPayload): IO[BlockUploadUrlResponse] =
    for {
      auJwt: AnyUserJwt <- jwt.asAnyUser
      _ <- (payload.chunkSize <= 4194304).orBadRequest("Invalid chunk size")
      obj <- storageObjectRepo.findById(auJwt.appId, payload.dataId).orNotFound
      _ <- obj.isOwner(auJwt).orForbidden
      block <- UUIDGen.randomString.map(StorageBlock(auJwt.appId, obj.id, _))
      _ <- storageBlockRepo.insert(block)
      signed <- AzureStorage.signBlockUpload(obj.id, block.idB64, payload.chunkSize)
    } yield BlockUploadUrlResponse(
      signed.authorization, signed.date, block.idB64, signed.url
    )
  
  def finishUpload(jwt: AuthJwt)(payload: FinishUploadPayload): IO[Boolean] =
    for {
      auJwt: AnyUserJwt <- jwt.asAnyUser
      _ <- payload.blockIds.nonEmpty.orBadRequest("Empty blockIds")
      nel = NonEmptyList.fromListUnsafe(payload.blockIds.distinct)
      obj <- storageObjectRepo.findById(auJwt.appId, payload.dataId).orNotFound
      _ <- obj.isOwner(auJwt).orForbidden
      _ <- storageBlockRepo.countByIds(auJwt.appId, obj.id, nel.map(decodeB64))
        .map(_ == nel.size).flatMap(_.orBadRequest("Bad blockId"))
      _ <- AzureStorage.finishBlockBlob(obj.id, payload.blockIds)
    } yield true
  
  def getFileUrl(jwt: AuthJwt)(objectId: String): IO[FileDownloadUrlResponse] =
    for {
      uJwt: UserJwt <- jwt.asUser
      obj <- storageObjectRepo.findById(uJwt.appId, objectId).orNotFound
      _ <- docKeyRepo.findByDocumentAndUser(uJwt.appId, objectId, uJwt.userId).orNotFound
      signed <- AzureStorage.signBlobDownload(obj.id)
    } yield FileDownloadUrlResponse(
      signed.authorization, signed.date, signed.url
    )
  
  def setMetadata(jwt: AuthJwt)(payload: SetMetadataPayload): IO[Boolean] =
    for {
      auJwt: AnyUserJwt <- jwt.asAnyUser
      obj <- storageObjectRepo.findById(auJwt.appId, payload.dataID).orNotFound
      _ <- obj.isOwner(auJwt).orForbidden
      _ <- storageObjectRepo.updateMetadataById(auJwt.appId, obj.id, payload.metadata)
    } yield true
  
  def getMetadata(jwt: AuthJwt)(dataId: String): IO[Option[String]] =
    for {
      uJwt: UserJwt <- jwt.asUser
      obj <- storageObjectRepo.findById(uJwt.appId, dataId).orNotFound
      _ <- docKeyRepo.findByDocumentAndUser(uJwt.appId, dataId, uJwt.userId).orNotFound
    } yield obj.meta
  
  private def decodeB64(in: String): String = String(Base64.getDecoder.decode(in))
}
