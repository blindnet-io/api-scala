package io.blindnet.backend
package endpoints

import auth.JwtAuthenticator
import objects.*
import services.MessageService

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*

class MessageEndpoints(auth: JwtAuthenticator, service: MessageService) {
  private val base = auth.secureEndpoint.tag("Messages")

  val sendMessage: ApiEndpoint =
    base.summary("Send Message (FR-M01)")
      .post
      .in("messages")
      .in(jsonBody[SendMessagePayload])
      .out(stringBody)
      .serverLogicSuccess(service.sendMessage)

  val getMessageIds: ApiEndpoint =
    base.summary("Get Message IDs (FR-M03)")
      .get
      .in("messages")
      .in(query[String]("deviceID"))
      .out(jsonBody[List[Long]])
      .serverLogicSuccess(service.getMessageIds)

  val getMessageContent: ApiEndpoint =
    base.summary("Get Message Content (FR-M04)")
      .get
      .in("messages" / "content")
      .in(query[String]("deviceID"))
      .in(query[List[String]]("messageIDs"))
      .out(jsonBody[List[MessageResponse]])
      .serverLogicSuccess(service.getMessageContent)

  val deleteAllUserMessages: ApiEndpoint =
    base.summary("Delete All User Messages (FR-M07)")
      .delete
      .in("messages")
      .serverLogicSuccess(service.deleteAllUserMessages)

  val saveBackup: ApiEndpoint =
    base.summary("Save Backup (FR-M10)")
      .post
      .in("messages" / "backup")
      .in(query[Boolean]("newBackup"))
      .in(query[Option[String]]("salt"))
      .in(streamBinaryBody(Fs2Streams[IO])(CodecFormat.OctetStream()))
      .serverLogicSuccess(service.saveBackup)

  val getBackup: ApiEndpoint =
    base.summary("Get Backup (FR-M11)")
      .get
      .in("messages" / "backup")
      .out(streamBinaryBody(Fs2Streams[IO])(CodecFormat.OctetStream()))
      .serverLogicSuccess(service.getBackup)

  val getBackupSalt: ApiEndpoint =
    base.summary("Get Backup Salt (FR-M11)")
      .get
      .in("messages" / "backup" / "salt")
      .out(jsonBody[String])
      .serverLogicSuccess(service.getBackupSalt)

  val list: List[ApiEndpoint] = List(
    sendMessage,
    getMessageIds,
    getMessageContent,
    deleteAllUserMessages,
    saveBackup,
    getBackup,
    getBackupSalt,
  )
}
