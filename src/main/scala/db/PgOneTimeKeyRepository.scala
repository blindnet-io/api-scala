package io.blindnet.backend
package db

import models.{OneTimeKey, OneTimeKeyRepository}

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgOneTimeKeyRepository(xa: Transactor[IO]) extends OneTimeKeyRepository[IO] {
  override def findByDevice(appId: String, userId: String, deviceId: String): IO[Option[OneTimeKey]] =
    sql"""select app_id, user_id, device_id, id, key from one_time_keys where app_id=$appId::uuid and user_id=$userId and device_id=$deviceId limit 1"""
      .query[OneTimeKey].option.transact(xa)

  override def insert(key: OneTimeKey): IO[Unit] =
    sql"""insert into one_time_keys (app_id, user_id, device_id, id, key)
          values (${key.appId}::uuid, ${key.userId}, ${key.deviceId}, ${key.id}, ${key.key})"""
      .update.run.transact(xa).void

  override def insertMany(keys: List[OneTimeKey]): IO[Unit] =
    Update[OneTimeKey]("insert into one_time_keys (app_id, user_id, device_id, id, key) values (?::uuid, ?, ?, ?, ?)")
      .updateMany(keys).transact(xa).void

  override def deleteById(appId: String, userId: String, deviceId: String, id: String): IO[Unit] =
    sql"delete from one_time_keys where app_id=$appId::uuid and user_id=$userId and device_id=$deviceId and id=$id"
      .update.run.transact(xa).void

  override def deleteAllByDevice(appId: String, userId: String, deviceId: String): IO[Unit] =
    sql"delete from one_time_keys where app_id=$appId::uuid and user_id=$userId and device_id=$deviceId"
      .update.run.transact(xa).void
}
