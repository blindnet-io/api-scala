package io.blindnet.backend
package objects

import models.UserKeys

case class CreateUserPayload(
  publicEncryptionKey: String,
  publicSigningKey: String,
  signedJwt: String,
  encryptedPrivateEncryptionKey: String,
  encryptedPrivateSigningKey: String,
  keyDerivationSalt: String,
  signedPublicEncryptionKey: String,
)

case class UpdateUserPrivateKeysPayload(
  encryptedPrivateEncryptionKey: String,
  encryptedPrivateSigningKey: String,
  keyDerivationSalt: Option[String],
)

case class UserKeysResponse(
  userID: String,
  publicEncryptionKey: String,
  publicSigningKey: String,
  encryptedPrivateEncryptionKey: String,
  encryptedPrivateSigningKey: String,
  keyDerivationSalt: String,
  signedPublicEncryptionKey: String,
)

case class UserPublicKeysResponse(
  userID: String,
  publicEncryptionKey: String,
  publicSigningKey: String,
  signedPublicEncryptionKey: String,
)
object UserPublicKeysResponse {
  def apply(keys: UserKeys): UserPublicKeysResponse = UserPublicKeysResponse(keys.userId, keys.publicEncKey, keys.publicSignKey, keys.signedPublicEncKey)
}

case class UsersPublicKeysPayload(userIds: Option[List[String]], groupId: Option[String])
