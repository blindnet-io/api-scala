package io.blindnet.backend
package services

import auth.*
import azure.AzureStorage
import errors.*
import models.*
import objects.*

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.*
import cats.effect.std.UUIDGen
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import fs2.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.*

import java.time.Instant
import java.util.{Random, UUID}
import scala.util.Try

class MessageService(deviceRepo: UserDeviceRepository[IO], messageRepo: MessageRepository[IO], backupRepo: MessageBackupRepository[IO]) {
  implicit val uuidGen: UUIDGen[IO] = UUIDGen.fromSync

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

  def saveBackup(jwt: AuthJwt)(newBackup: Boolean, saltOpt: Option[String], stream: Stream[IO, Byte]): IO[Unit] =
    for {
      uJwt: UserJwt <- jwt.asUser
      _ <- if newBackup
      then for {
        salt <- saltOpt.orBadRequest("salt required when newBackup=true")
        _ <- backupRepo.deleteByUserId(uJwt.appId, uJwt.userId)
        backup <- UUIDGen.randomString.map(MessageBackup(uJwt.appId, uJwt.userId, _, salt))
        _ <- backupRepo.insert(backup)
        _ <- stream.through(AzureStorage.upload(backup.blobId)).compile.drain
      } yield ()
      else for {
        _ <- saltOpt.thenBadRequest("salt not supported when newBackup=false")
        backup <- backupRepo.findByUserId(uJwt.appId, uJwt.userId).orNotFound
        _ <- stream.through(AzureStorage.upload(backup.blobId)).compile.drain
      } yield ()
    } yield ()

  def getBackup(jwt: AuthJwt)(x: Unit): IO[Stream[IO, Byte]] =
    for {
      uJwt: UserJwt <- jwt.asUser
      backup <- backupRepo.findByUserId(uJwt.appId, uJwt.userId).orNotFound
    } yield AzureStorage.download(backup.blobId)

  def getBackupSalt(jwt: AuthJwt)(x: Unit): IO[String] =
    for {
      uJwt: UserJwt <- jwt.asUser
      backup <- backupRepo.findByUserId(uJwt.appId, uJwt.userId).orNotFound
    } yield backup.salt
}
