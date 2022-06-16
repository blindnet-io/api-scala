package io.blindnet.backend
package objects

import models.*

case class CreateSignalUserPayload(
  deviceID: String,
  publicIkID: String,
  publicIk: String,
  publicSpkID: String,
  publicSpk: String,
  pkSig: String,
  signedJwt: String,
  signalOneTimeKeys: List[OneTimeKeyPayload],
)

case class OneTimeKeyPayload(
  publicOpkID: String,
  publicOpk: String,
)

case class UpdateSignalUserPayload(
  deviceID: String,
  publicSpkID: Option[String],
  publicSpk: Option[String],
  pkSig: Option[String],
  signalOneTimeKeys: Option[List[OneTimeKeyPayload]],
)

case class UserKeysResponseItem(
  userID: String,
  deviceID: String,
  publicIkID: String,
  publicIk: String,
  publicSpkID: String,
  publicSpk: String,
  pkSig: String,
  publicOpkID: Option[String],
  publicOpk: Option[String],
)
object UserKeysResponseItem {
  def apply(device: UserDevice, otKey: Option[OneTimeKey] = None): UserKeysResponseItem =
    new UserKeysResponseItem(
      device.userId, device.id,
      device.publicIkId, device.publicIk,
      device.publicSpkId, device.publicSpk,
      device.pkSig,
      otKey.map(_.id), otKey.map(_.key)
    )
}

case class UserDevicesResponseItem(
  userID: String,
  deviceID: String,
)
object UserDevicesResponseItem {
  def apply(device: UserDevice): UserDevicesResponseItem =
    new UserDevicesResponseItem(device.userId, device.id)
}
