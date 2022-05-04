package io.blindnet.backend
package util

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.{Ed25519KeyGenerationParameters, Ed25519PrivateKeyParameters, Ed25519PublicKeyParameters}
import org.bouncycastle.crypto.signers.Ed25519Signer

import java.security.SecureRandom
import java.util.Base64

object EdUtil {
  def createKeyPair(): EdKeyPair = {
    val gen = Ed25519KeyPairGenerator()
    gen.init(Ed25519KeyGenerationParameters(SecureRandom()))
    val pair = gen.generateKeyPair()
    EdKeyPair(
      pair.getPublic.asInstanceOf[Ed25519PublicKeyParameters],
      pair.getPrivate.asInstanceOf[Ed25519PrivateKeyParameters]
    )
  }
}

case class EdKeyPair(publicKey: Ed25519PublicKeyParameters, privateKey: Ed25519PrivateKeyParameters) extends ToUnique[EdKeyPair] {
  val publicKeyBytes: Array[Byte] = publicKey.getEncoded
  val publicKeyString: String = Base64.getEncoder.encodeToString(publicKeyBytes)
  val privateKeyBytes: Array[Byte] = privateKey.getEncoded
  val privateKeyString: String = Base64.getEncoder.encodeToString(privateKeyBytes)

  def sign(data: Array[Byte]): Array[Byte] = {
    val signer = Ed25519Signer()
    signer.init(true, privateKey)
    signer.update(data, 0, data.length)
    signer.generateSignature()
  }

  def signToString(data: Array[Byte]): String =
    Base64.getEncoder.encodeToString(sign(data))
  
  def verify(data: Array[Byte], signature: Array[Byte]): Boolean = {
    val signer = Ed25519Signer()
    signer.init(false, publicKey)
    signer.update(data, 0, data.length)
    signer.verifySignature(signature)
  }
  
  def verifyFromString(data: Array[Byte], signature: String): Boolean =
    verify(data, Base64.getDecoder.decode(signature))
}
