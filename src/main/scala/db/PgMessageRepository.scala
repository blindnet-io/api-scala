package io.blindnet.backend
package db

import models.{Message, MessageRepository}

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgMessageRepository(xa: Transactor[IO]) extends MessageRepository[IO] {
  override def findById(appId: String, id: Long): IO[Option[Message]] =
    sql"""select id, app_id, sender_id, sender_device_id, recipient_id, recipient_device_id, data, dh_key, public_ik, public_ek, time_sent, time_delivered, time_read from messages
          where app_id=$appId::uuid and id=$id"""
      .query[Message].option.transact(xa)

  override def findAllByRecipientAndIds(appId: String, recipientId: String, deviceId: String, ids: List[String]): IO[List[Message]] =
    NonEmptyList.fromList(ids) match
      case Some(nel) => (fr"select id, app_id, sender_id, sender_device_id, recipient_id, recipient_device_id, data, dh_key, public_ik, public_ek, time_sent, time_delivered, time_read from messages"
        ++ fr"where app_id=$appId::uuid and recipient_id=$recipientId and recipient_device_id=$deviceId and"
        ++ Fragments.in(fr"id", nel))
        .query[Message].to[List].transact(xa)
      case None => IO.pure(Nil)

  override def findAllIdsByRecipient(appId: String, recipientId: String, deviceId: String): IO[List[Long]] =
    sql"select id from messages where app_id=$appId::uuid and recipient_id=$recipientId and recipient_device_id=$deviceId"
      .query[Long].to[List].transact(xa)
  
  override def insert(message: Message): IO[Unit] =
    sql"""insert into messages (id, app_id, sender_id, sender_device_id, recipient_id, recipient_device_id, data, dh_key, public_ik, public_ek, time_sent, time_delivered, time_read)
          values (${message.id}, ${message.appId}::uuid, ${message.senderId}, ${message.senderDeviceId}, ${message.recipientId}, ${message.recipientDeviceId},
                  ${message.data}, ${message.dhKey}, ${message.publicIk}, ${message.publicEk}, ${message.timeSent}, ${message.timeDelivered}, ${message.timeRead})"""
      .update.run.transact(xa).map(_ => ())

  override def deleteAllByUser(appId: String, userId: String): IO[Unit] =
    sql"delete from messages where app_id=$appId::uuid and (sender_id=$userId or recipient_id=$userId)"
      .update.run.transact(xa).void
}
