package io.blindnet.backend
package util

import org.apache.commons.lang3.RandomUtils

import java.nio.ByteBuffer
import java.security.{KeyPairGenerator, PrivateKey, PublicKey}
import java.util.Base64
import javax.crypto.spec.{GCMParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, SecretKey, SecretKeyFactory}

object AesUtil {
  def createKey(password: String, salt: Array[Byte]): AesKey = {
    val gen = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password.toCharArray, salt, 100000, 256)
    AesKey(
      SecretKeySpec(gen.generateSecret(spec).getEncoded, "AES"),
      salt
    )
  }
}

case class AesKey(secretKey: SecretKey, salt: Array[Byte]) {
  def encrypt(data: Array[Byte]): Array[Byte] = {
    val iv = RandomUtils.nextBytes(12)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
    val encrypted = cipher.doFinal(data)
    ByteBuffer.allocate(iv.length + encrypted.length)
      .put(iv).put(encrypted).array()
  }

  def encryptToString(data: Array[Byte]): String =
    Base64.getEncoder.encodeToString(encrypt(data))

  def decrypt(data: Array[Byte]): Array[Byte] = {
    val iv = new Array[Byte](12)
    val encrypted = new Array[Byte](data.length - 12)
    ByteBuffer.wrap(data).get(iv).get(encrypted)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
    cipher.doFinal(encrypted)
  }

  def decryptFromString(data: String): Array[Byte] =
    decrypt(Base64.getDecoder.decode(data))
}