package io.blindnet.backend
package db

import models.{MessageBackup, MessageBackupRepository}

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgMessageBackupRepository(xa: Transactor[IO]) extends MessageBackupRepository[IO] {
  override def findByUserId(appId: String, userId: String): IO[Option[MessageBackup]] =
    sql"""select app_id, user_id, id, salt from message_backups
          where app_id=$appId::uuid and user_id=$userId"""
      .query[MessageBackup].option.transact(xa)

  override def insert(backup: MessageBackup): IO[Unit] =
    sql"""insert into message_backups (app_id, user_id, id, salt)
          values (${backup.appId}::uuid, ${backup.userId}, ${backup.id}::uuid, ${backup.salt})"""
      .update.run.transact(xa).void

  override def deleteByUserId(appId: String, userId: String): IO[Unit] =
    sql"delete from message_backups where app_id=$appId::uuid and user_id=$userId"
      .update.run.transact(xa).void
}
