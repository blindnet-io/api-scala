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
  override def insert(key: OneTimeKey): IO[Unit] =
    sql"""insert into one_time_keys (app_id, user_id, device_id, id, key)
          values (${key.appId}::uuid, ${key.userId}, ${key.deviceId}, ${key.id}, ${key.key})"""
      .update.run.transact(xa).map(_ => ())
}
