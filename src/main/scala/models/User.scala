package io.blindnet.backend
package models

case class User(
  appId: String,
  id: String,
  publicEncKey: String,
  publicSignKey: String,
  signedPublicEncKey: String,
  encPrivateEncKey: String,
  encPrivateSignKey: String,
  keyDerivationSalt: String,
)

trait UserRepository[F[_]] {
  def findById(id: String): F[Option[User]]
  def insert(user: User): F[Unit]
  def updatePrivateKeys(id: String, encPrivateEncKey: String, encPrivateSignKey: String): F[Unit]
  def updatePrivateKeysAndSalt(id: String, encPrivateEncKey: String, encPrivateSignKey: String, keyDerivationSalt: String): F[Unit]
  def delete(id: String): F[Unit]
}
