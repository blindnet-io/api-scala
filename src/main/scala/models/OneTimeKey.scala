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
  def insert(key: OneTimeKey): F[Unit]
  def deleteByDevice(appId: String, userId: String, deviceId: String): F[Unit]
}
