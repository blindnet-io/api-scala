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
    sql"""select id, app_id, sender_id, recipient_id, data, time_sent from messages
          where app_id=$appId::uuid and id=$id"""
      .query[Message].option.transact(xa)
  
  override def insert(message: Message): IO[Unit] =
    sql"""insert into messages (id, app_id, sender_id, recipient_id, data, time_sent)
          values (${message.id}, ${message.appId}::uuid, ${message.senderId}, ${message.recipientId}, ${message.data}, ${message.timeSent})"""
      .update.run.transact(xa).map(_ => ())
}
