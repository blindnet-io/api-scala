package io.blindnet.backend
package db

import models.{StorageObject, StorageObjectRepository}

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgStorageObjectRepository(xa: Transactor[IO]) extends StorageObjectRepository[IO] {
  override def insert(obj: StorageObject): IO[Unit] =
    sql"""insert into storage_objects (app_id, id, user_id, token_id, meta)
          values (${obj.appId}::uuid, ${obj.id}::uuid, ${obj.userId}, ${obj.tokenId}, ${obj.meta})"""
      .update.run.transact(xa).void
}
