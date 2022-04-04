package io.blindnet.backend
package util

import cats.effect.IO
import pdi.jwt.JwtBase64

import java.util.{Date, UUID}

class TestApp(val id: String = UUID.randomUUID().toString) {
  val key: EdKeyPair = EdUtil.createKeyPair()

  def createUserToken(appId: String, userId: String, groupId: String): String =
    val exp = Date(System.currentTimeMillis() + 60*60*1000)
    val header = s"{\"alg\":\"EdDSA\",\"typ\":\"jwt\"}"
    val payload = s"{\"app\":\"$appId\",\"uid\":\"$userId\",\"exp\":\"$exp\",\"gid\":\"$groupId\"}"
    val hd = JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(payload)
    hd + "." + JwtBase64.encodeString(key.sign(hd.getBytes))

  def insert(app: ServerApp): IO[Unit] =
    app.appRepo.insert(models.App(id, key.publicKeyString, "Test App " + id.substring(0, 8)))
}
