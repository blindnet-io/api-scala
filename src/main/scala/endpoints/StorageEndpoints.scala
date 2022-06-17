package io.blindnet.backend
package endpoints

import auth.JwtAuthenticator
import objects.*
import services.StorageService

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*

class StorageEndpoints(auth: JwtAuthenticator, service: StorageService) {
  private val base = auth.secureEndpoint.tag("Storage")

  val initUpload: ApiEndpoint =
    base.summary("Init Upload")
      .post
      .in("init-upload")
      .out(jsonBody[InitUploadResponse])
      .serverLogicSuccess(service.initUpload)

  val getBlockUploadUrl: ApiEndpoint =
    base.summary("Get Block Upload URL")
      .post
      .in("get-upload-block-url")
      .in(jsonBody[BlockUploadUrlPayload])
      .out(jsonBody[BlockUploadUrlResponse])
      .serverLogicSuccess(service.getBlockUploadUrl)

  val finishUpload: ApiEndpoint =
    base.summary("Finish Upload")
      .post
      .in("finish-upload")
      .in(jsonBody[FinishUploadPayload])
      .out(jsonBody[Boolean])
      .serverLogicSuccess(service.finishUpload)

  val getFileUrl: ApiEndpoint =
    base.summary("Get File URL")
      .get
      .in("get-file-url" / path[String]("objectId"))
      .out(jsonBody[FileDownloadUrlResponse])
      .serverLogicSuccess(service.getFileUrl)

  val setMetadata: ApiEndpoint =
    base.summary("Set Metadata")
      .post
      .in("metadata")
      .in(jsonBody[SetMetadataPayload])
      .out(jsonBody[Boolean])
      .serverLogicSuccess(service.setMetadata)

  val getMetadata: ApiEndpoint =
    base.summary("Get Metadata")
      .get
      .in("metadata")
      .in(query[String]("dataId"))
      .out(jsonBody[Option[String]])
      .serverLogicSuccess(service.getMetadata)

  val list: List[ApiEndpoint] = List(
    initUpload,
    getBlockUploadUrl,
    finishUpload,
    getFileUrl,
    setMetadata,
    getMetadata,
  )
}
