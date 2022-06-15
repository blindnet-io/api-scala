package io.blindnet.backend
package endpoints

import auth.JwtAuthenticator
import objects.*
import services.MessageService

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*

class MessageEndpoints(auth: JwtAuthenticator, service: MessageService) {
  private val base = auth.secureEndpoint.tag("Messages")

  val sendMessage: ServerEndpoint[Any, IO] =
    base.summary("Send Message (FR-M01)")
      .post
      .in("messages")
      .in(jsonBody[SendMessagePayload])
      .out(stringBody)
      .serverLogicSuccess(service.sendMessage)

  val getMessageIds: ServerEndpoint[Any, IO] =
    base.summary("Get Message IDs (FR-M03)")
      .get
      .in("messages")
      .in(query[String]("deviceID"))
      .out(jsonBody[List[Long]])
      .serverLogicSuccess(service.getMessageIds)

  val getMessageContent: ServerEndpoint[Any, IO] =
    base.summary("Get Message Content (FR-M04)")
      .get
      .in("messages" / "content")
      .in(query[String]("deviceID"))
      .in(query[List[String]]("messageIDs"))
      .out(jsonBody[List[MessageResponse]])
      .serverLogicSuccess(service.getMessageContent)

  val deleteAllUserMessages: ServerEndpoint[Any, IO] =
    base.summary("Delete All User Messages (FR-M07)")
      .delete
      .in("messages")
      .serverLogicSuccess(service.deleteAllUserMessages)

  val list: List[ServerEndpoint[Any, IO]] = List(
    sendMessage,
    getMessageIds,
    getMessageContent,
    deleteAllUserMessages,
  )
}
