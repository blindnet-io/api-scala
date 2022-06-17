package io.blindnet.backend
package models

case class MessageBackup(
  appId: String,
  userId: String,
  id: String,
  salt: String,
) {
  def blobId: String = userId + "/" + id
}

trait MessageBackupRepository[F[_]] {
  def findByUserId(appId: String, userId: String): F[Option[MessageBackup]]
  def insert(backup: MessageBackup): F[Unit]
  def deleteByUserId(appId: String, userId: String): F[Unit]
}
