package io.blindnet.backend
package util

import org.apache.commons.lang3.RandomUtils

import java.nio.ByteBuffer
import java.security.{KeyPairGenerator, PrivateKey, PublicKey}
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

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

  def encrypt(data: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance("RSA/NONE/OAEPPadding")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    cipher.doFinal(data)
  }

  def encryptToString(data: Array[Byte]): String =
    Base64.getEncoder.encodeToString(encrypt(data))

  def decrypt(data: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance("RSA/NONE/OAEPPadding")
    cipher.init(Cipher.DECRYPT_MODE, privateKey)
    cipher.doFinal(data)
  }
  
  def decryptFromString(data: String): Array[Byte] =
    decrypt(Base64.getDecoder.decode(data))
}