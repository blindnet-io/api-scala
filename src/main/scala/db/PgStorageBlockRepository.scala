package io.blindnet.backend
package db

import models.{StorageBlock, StorageBlockRepository}

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgStorageBlockRepository(xa: Transactor[IO]) extends StorageBlockRepository[IO] {
  override def countByIds(appId: String, objectId: String, ids: NonEmptyList[String]): IO[Long] =
    (fr"select count(*) from storage_blocks where app_id=$appId::uuid and object_id=$objectId::uuid and"
      ++ DbUtil.Fragments.inUuid(fr"id", ids))
      .query[Long].unique.transact(xa)

  override def insert(block: StorageBlock): IO[Unit] =
    sql"""insert into storage_blocks (app_id, object_id, id)
          values (${block.appId}::uuid, ${block.objectId}::uuid, ${block.id})"""
      .update.run.transact(xa).void
}
