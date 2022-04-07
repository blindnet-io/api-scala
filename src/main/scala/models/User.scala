package io.blindnet.backend
package models

case class User(
  appId: String,
  id: String,
  groupId: String,
  publicEncKey: String,
  publicSignKey: String,
  signedPublicEncKey: String,
  encPrivateEncKey: String,
  encPrivateSignKey: String,
  keyDerivationSalt: String,
)

trait UserRepository[F[_]] {
  def countByIdsOutsideGroup(appId: String, groupId: String, usersId: List[String]): F[Long]
  def findAllByGroup(appId: String, groupId: String): F[List[User]]
  def findById(appId: String, id: String): F[Option[User]]
  def insert(user: User): F[Unit]
  def updatePrivateKeys(appId: String, id: String, encPrivateEncKey: String, encPrivateSignKey: String): F[Unit]
  def updatePrivateKeysAndSalt(appId: String, id: String, encPrivateEncKey: String, encPrivateSignKey: String, keyDerivationSalt: String): F[Unit]
  def delete(appId: String, id: String): F[Unit]
  def deleteAllByGroup(appId: String, groupId: String): F[Unit]
}
