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

import java.time.Instant
import java.util.{Random, UUID}
import scala.util.Try

class MessageService(userRepo: UserRepository[IO], deviceRepo: UserDeviceRepository[IO], messageRepo: MessageRepository[IO]) {
  def authedRoutes: AuthedRoutes[AuthJwt, IO] = AuthedRoutes.of[AuthJwt, IO] {
    // FR-M01 Send Message
    case req @ POST -> Root / "messages" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[SendMessagePayload]

        recipient <- deviceRepo.findById(uJwt.appId, payload.recipientID, payload.recipientDeviceID).orNotFound
        sender <- deviceRepo.findById(uJwt.appId, uJwt.userId, payload.senderDeviceID).orNotFound

        timeSent <- Try(Instant.parse(payload.timestamp)).orBadRequest("Bad timestamp")
        msg = models.Message(
          Random().nextLong(), uJwt.appId,
          sender.userId, sender.id,
          recipient.userId, recipient.id,
          payload.message, payload.dhKey,
          payload.senderKeys.publicIk, payload.senderKeys.publicEk,
          timeSent
        )
        _ <- messageRepo.insert(msg)
        res <- Ok(msg.id.toString)
      } yield res

    // FR-M03 Get Message IDs
    case req @ GET -> Root / "messages" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        deviceId <- req.req.params.get("deviceID").orBadRequest("Missing deviceID")
        res <- Ok(messageRepo.findAllIdsByRecipient(uJwt.appId, uJwt.userId, deviceId))
      } yield res

    // FR-M04 Get Message Content
    case req @ GET -> Root / "messages" / "content" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        deviceId <- req.req.params.get("deviceID").orBadRequest("Missing deviceID")
        messageIds <- req.req.multiParams.get("messageIDs").orBadRequest("Missing messageIDs")
        messages <- messageRepo.findAllByRecipientAndIds(uJwt.appId, uJwt.userId, deviceId, messageIds.toList)
        res <- Ok(messages.map(MessageResponse.apply))
      } yield res
  }
}

case class SendMessagePayload(
  recipientID: String,
  recipientDeviceID: String,
  senderDeviceID: String,
  message: String,
  dhKey: String,
  senderKeys: SenderKeys,
  timestamp: String,
)

case class SenderKeys(
  publicIk: String,
  publicEk: String,
)

case class MessageResponse(
  id: String,
  senderID: String,
  recipientID: String,
  senderDeviceID: String,
  recipientDeviceID: String,
  messageContent: String,
  dhKey: String,
  timeSent: String,
  timeDelivered: Option[String],
  timeRead: Option[String],
  messageSenderKeys: MessageSenderKeys,
)
object MessageResponse {
  def apply(message: models.Message): MessageResponse = new MessageResponse(
    message.id.toString,
    message.senderId, message.recipientId,
    message.senderDeviceId, message.recipientDeviceId,
    message.data, message.dhKey,
    message.timeSent.toString, message.timeDelivered.map(_.toString), message.timeRead.map(_.toString),
    MessageSenderKeys(message)
  )
}

case class MessageSenderKeys(
  publicIk: String,
  publicEk: String,
  messageID: String,
)
object MessageSenderKeys {
  def apply(message: models.Message): MessageSenderKeys = new MessageSenderKeys(
    message.publicIk, message.publicEk,
    message.id.toString
  )
}
