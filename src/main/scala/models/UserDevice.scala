package io.blindnet.backend
package models

import cats.data.NonEmptyList

case class UserDevice(
  appId: String,
  userId: String,
  id: String,
  publicIkId: String,
  publicIk: String,
  publicSpkId: String,
  publicSpk: String,
  pkSig: String,
)

trait UserDeviceRepository[F[_]] {
  def findById(appId: String, userId: String, id: String): F[Option[UserDevice]]
  def findAllByUser(appId: String, userId: String): F[List[UserDevice]]
  def findAllByUserAndIds(appId: String, userId: String, ids: NonEmptyList[String]): F[List[UserDevice]]
  def insert(userDevice: UserDevice): F[Unit]
  def updateSpkById(appId: String, userId: String, id: String, spkId: String, spk: String, pkSig: String): F[Unit]
}
