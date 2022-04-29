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
          timeSent
        )
        _ <- messageRepo.insert(msg)
        res <- Ok(msg.id.toString)
      } yield res
  }
}

case class SendMessagePayload(
  recipientID: String,
  recipientDeviceID: String,
  senderDeviceID: String,
  message: String,
  dhKey: String,
  senderKeys: List[SenderKeys],
  timestamp: String,
)

case class SenderKeys(
  publicIk: String,
  publicEk: String,
)
