package io.blindnet.backend
package models

case class User(
  appId: String,
  id: String,
  publicEncKey: String,
  publicSignKey: String,
  signedPublicEncKey: Option[String],
  encPrivateEncKey: Option[String],
  encPrivateSignKey: Option[String],
  keyDerivationSalt: Option[String],
)

trait UserRepository[F[_]] {
  def findById(id: String): F[Option[User]]
  def insert(user: User): F[Unit]
  def updatePrivateKeys(id: String, encPrivateEncKey: String, encPrivateSignKey: String, keyDerivationSalt: Option[String]): F[Unit]
  def delete(id: String): F[Unit]
}
