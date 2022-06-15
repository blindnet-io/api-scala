package io.blindnet.backend
package services

import auth.*
import errors.*
import models.*
import objects.*

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
  def sendMessage(jwt: AuthJwt)(payload: SendMessagePayload): IO[String] =
    for {
      uJwt: UserJwt <- jwt.asUser

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
    } yield msg.id.toString

  def getMessageIds(jwt: AuthJwt)(deviceId: String): IO[List[Long]] =
    for {
      uJwt: UserJwt <- jwt.asUser
      ids <- messageRepo.findAllIdsByRecipient(uJwt.appId, uJwt.userId, deviceId)
    } yield ids

  def getMessageContent(jwt: AuthJwt)(deviceId: String, messageIds: List[String]): IO[List[MessageResponse]] =
    for {
      uJwt: UserJwt <- jwt.asUser
      messages <- messageRepo.findAllByRecipientAndIds(uJwt.appId, uJwt.userId, deviceId, messageIds)
    } yield messages.map(MessageResponse.apply)

  def deleteAllUserMessages(jwt: AuthJwt)(x: Unit): IO[Unit] =
    for {
      uJwt: UserJwt <- jwt.asUser
      _ <- messageRepo.deleteAllByUser(uJwt.appId, uJwt.userId)
    } yield ()
}
