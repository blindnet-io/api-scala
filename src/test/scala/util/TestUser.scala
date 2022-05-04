package io.blindnet.backend
package util

import func.user.CreateUserSpec
import models.User

import cats.effect.IO
import org.http4s.*
import org.apache.commons.lang3.{RandomStringUtils, RandomUtils}

import java.util.{Base64, UUID}

class TestUser(
    val id: String = UUID.randomUUID().toString,
    val group: String = "",
    val encKey: RsaKeyPair = RsaUtil.createKeyPair(),
    val sigKey: EdKeyPair = EdUtil.createKeyPair()) {
  val password: String = RandomStringUtils.random(16)
  val salt: Array[Byte] = RandomUtils.nextBytes(16)
  val saltString: String = Base64.getEncoder.encodeToString(salt)
  val aes: AesKey = AesUtil.createKey(password, salt)

  def insert(serverApp: ServerApp, testApp: TestApp): IO[Unit] =
    serverApp.app.run(CreateUserSpec().createCompleteRequest(testApp, this, testApp.createUserToken(id, group))).void

  def changePasswordAndSalt(): TestUser = TestUser(id, group, encKey, sigKey)
  
  def device(): TestDevice = TestDevice(this)
}
