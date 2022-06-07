package io.blindnet.backend
package models

import auth.{AnyUserJwt, TempUserJwt, UserJwt}

case class StorageObject(
  appId: String,
  id: String,
  userId: Option[String],
  tokenId: Option[String],
  meta: Option[String] = None
) {
  def isOwner(jwt: AnyUserJwt): Boolean = jwt match
      case uJwt: UserJwt => userId.contains(uJwt.userId)
      case tuJwt: TempUserJwt => tokenId.contains(tuJwt.tokenId)
}

trait StorageObjectRepository[F[_]] {
  def findById(appId: String, id: String): F[Option[StorageObject]]
  def insert(obj: StorageObject): F[Unit]
  def updateMetadataById(appId: String, id: String, metadata: String): F[Unit]
}
