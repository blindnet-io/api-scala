package io.blindnet.backend
package models

case class UserDevice(
  appId: String,
  userId: String,
  id: String,
  signingPublicKey: String,
  publicIkId: String,
  publicIk: String,
  publicSpkId: String,
  publicSpk: String,
  pkSig: String,
)

trait UserDeviceRepository[F[_]] {
  def findById(appId: String, userId: String, id: String): F[Option[UserDevice]]
  def insert(userDevice: UserDevice): F[Unit]
  def updateSpkById(appId: String, userId: String, id: String, spkId: String, spk: String, pkSig: String): F[Unit]
}
