package io.blindnet.backend
package models

import cats.data.NonEmptyList

case class StorageBlock(
  appId: String,
  objectId: String,
  id: String,
)

trait StorageBlockRepository[F[_]] {
  def countByIds(appId: String, objectId: String, ids: NonEmptyList[String]): F[Long]
  def insert(obj: StorageBlock): F[Unit]
}
