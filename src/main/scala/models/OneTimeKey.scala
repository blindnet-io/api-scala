package io.blindnet.backend
package models

case class OneTimeKey(
  appId: String,
  userId: String,
  deviceId: String,
  id: String,
  key: String,
)

trait OneTimeKeyRepository[F[_]] {
  def findByDevice(appId: String, userId: String, deviceId: String): F[Option[OneTimeKey]]
  def findAllByDevice(appId: String, userId: String, deviceId: String): F[List[OneTimeKey]]
  def insert(key: OneTimeKey): F[Unit]
  def insertMany(keys: List[OneTimeKey]): F[Unit]
  def deleteById(appId: String, userId: String, deviceId: String, id: String): F[Unit]
  def deleteAllByDevice(appId: String, userId: String, deviceId: String): F[Unit]
}
