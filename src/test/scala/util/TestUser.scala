package io.blindnet.backend
package util

import org.apache.commons.lang3.{RandomStringUtils, RandomUtils}

import java.util.UUID

class TestUser(val id: String = UUID.randomUUID().toString) {
  val password: String = RandomStringUtils.random(16)
  val salt: Array[Byte] = RandomUtils.nextBytes(16)

  val encKey: RsaKeyPair = RsaUtil.createKeyPair()
  val sigKey: EdKeyPair = EdUtil.createKeyPair()
  val aes: AesKey = AesUtil.createKey(password, salt)
}
