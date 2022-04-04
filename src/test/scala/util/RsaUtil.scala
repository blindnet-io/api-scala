package io.blindnet.backend
package util

import java.security.{KeyPairGenerator, PrivateKey, PublicKey}
import java.util.Base64

object RsaUtil {
  def createKeyPair(): RsaKeyPair = {
    val gen = KeyPairGenerator.getInstance("RSA")
    gen.initialize(4096)
    val pair = gen.generateKeyPair()
    RsaKeyPair(
      pair.getPublic,
      pair.getPrivate
    )
  }
}

case class RsaKeyPair(publicKey: PublicKey, privateKey: PrivateKey) {
  val publicKeyBytes: Array[Byte] = publicKey.getEncoded
  val publicKeyString: String = Base64.getEncoder.encodeToString(publicKeyBytes)
  val privateKeyBytes: Array[Byte] = privateKey.getEncoded
  val privateKeyString: String = Base64.getEncoder.encodeToString(privateKeyBytes)
}