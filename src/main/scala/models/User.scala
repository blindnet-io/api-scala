package io.blindnet.backend
package models

case class User(
  id: String,
  publicEncKey: String,
  publicSignKey: String,
  encPrivateEncKey: Option[String],
  encPrivateSignKey: Option[String],
  keyDerivationSalt: Option[String],
)

trait UserRepository[F[_]] {
  def findById(id: String): F[Option[User]]
  def insert(user: User): F[Unit]
}
