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
  // FR-M01
  val sendMessage: ServerEndpoint[Any, IO] =
    auth.secureEndpoint
      .post
      .in("messages")
      .in(jsonBody[SendMessagePayload])
      .out(stringBody)
      .serverLogicSuccess(service.sendMessage)

  // FR-M03
  val getMessageIds: ServerEndpoint[Any, IO] =
    auth.secureEndpoint
      .get
      .in("messages")
      .in(query[String]("deviceID"))
      .out(jsonBody[List[Long]])
      .serverLogicSuccess(service.getMessageIds)

  // FR-M04
  val getMessageContent: ServerEndpoint[Any, IO] =
    auth.secureEndpoint
      .get
      .in("messages" / "content")
      .in(query[String]("deviceID"))
      .in(query[List[String]]("messageIDs"))
      .out(jsonBody[List[MessageResponse]])
      .serverLogicSuccess(service.getMessageContent)

  // FR-M07
  val deleteAllUserMessages: ServerEndpoint[Any, IO] =
    auth.secureEndpoint
      .delete
      .in("messages")
      .serverLogicSuccess(service.deleteAllUserMessages)

  def routes: HttpRoutes[IO] = Http4sServerInterpreter[IO]().toRoutes(List(
    sendMessage,
    getMessageIds,
    getMessageContent,
    deleteAllUserMessages,
  ))
}
