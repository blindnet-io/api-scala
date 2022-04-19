package io.blindnet.backend
package models

case class User(
  appId: String,
  id: String,
  groupId: String,
)

trait UserRepository[F[_]] {
  def countByIdsOutsideGroup(appId: String, groupId: String, usersId: List[String]): F[Long]
  def findById(appId: String, id: String): F[Option[User]]
  def findAllByGroup(appId: String, groupId: String): F[List[User]]
  def findAllByIds(appId: String, ids: List[String]): F[List[User]]
  def insert(user: User): F[Unit]
  def delete(appId: String, id: String): F[Unit]
  def deleteAllByGroup(appId: String, groupId: String): F[Unit]
}
