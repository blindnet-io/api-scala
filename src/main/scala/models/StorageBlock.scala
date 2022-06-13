package io.blindnet.backend
package models

import cats.data.NonEmptyList

import java.util.Base64

case class StorageBlock(
  appId: String,
  objectId: String,
  id: String,
) {
  val idB64: String = Base64.getEncoder.encodeToString(id.getBytes)
}

trait StorageBlockRepository[F[_]] {
  def countByIds(appId: String, objectId: String, ids: NonEmptyList[String]): F[Long]
  def insert(obj: StorageBlock): F[Unit]
}
