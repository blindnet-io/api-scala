package io.blindnet.backend
package models

case class StorageObject(
  appId: String,
  id: String,
  userId: Option[String],
  tokenId: Option[String],
  meta: Option[String] = None
)

trait StorageObjectRepository[F[_]] {
  def findById(appId: String, id: String): F[Option[StorageObject]]
  def insert(obj: StorageObject): F[Unit]
}
