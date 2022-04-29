package io.blindnet.backend
package models

case class UserKeys(
  appId: String,
  userId: String,
  publicEncKey: String,
  publicSignKey: String,
  signedPublicEncKey: String,
  encPrivateEncKey: String,
  encPrivateSignKey: String,
  keyDerivationSalt: String,
)

trait UserKeysRepository[F[_]] {
  def findById(appId: String, userId: String): F[Option[UserKeys]]
  def findAllByGroup(appId: String, groupId: String): F[List[UserKeys]]
  def findAllByIds(appId: String, ids: List[String]): F[List[UserKeys]]
  def insert(userKeys: UserKeys): F[Unit]
  def updatePrivateKeys(appId: String, userId: String, encPrivateEncKey: String, encPrivateSignKey: String): F[Unit]
  def updatePrivateKeysAndSalt(appId: String, userId: String, encPrivateEncKey: String, encPrivateSignKey: String, keyDerivationSalt: String): F[Unit]
}
