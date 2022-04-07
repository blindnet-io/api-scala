package io.blindnet.backend
package util

import cats.effect.IO
import io.circe.literal.*
import pdi.jwt.JwtBase64

import java.util.{Date, UUID}

class TestApp(val id: String = UUID.randomUUID().toString) {
  val key: EdKeyPair = EdUtil.createKeyPair()

  def createUserToken(testUser: TestUser): String =
    createUserToken(testUser.id, testUser.group)

  def createUserToken(userId: String, groupId: String): String =
    val exp = Date(System.currentTimeMillis() + 60*60*1000)
    val header = s"{\"alg\":\"EdDSA\",\"typ\":\"jwt\"}"
    val payload = s"{\"app\":\"$id\",\"uid\":\"$userId\",\"exp\":\"$exp\",\"gid\":\"$groupId\"}"
    val hd = JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(payload)
    hd + "." + JwtBase64.encodeString(key.sign(hd.getBytes))

  def createTempUserToken(groupId: String): String =
    val tid = UUID.randomUUID().toString
    val exp = Date(System.currentTimeMillis() + 60*60*1000)
    val header = s"{\"alg\":\"EdDSA\",\"typ\":\"tjwt\"}"
    val payload = s"{\"app\":\"$id\",\"tid\":\"$tid\",\"exp\":\"$exp\",\"gid\":\"$groupId\"}"
    val hd = JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(payload)
    hd + "." + JwtBase64.encodeString(key.sign(hd.getBytes))

  def createTempUserToken(userIds: List[String]): String =
    val tid = UUID.randomUUID().toString
    val exp = Date(System.currentTimeMillis() + 60*60*1000).toString
    val header = s"{\"alg\":\"EdDSA\",\"typ\":\"tjwt\"}"
    val payload = json"""{"app":$id,"tid":$tid,"exp":$exp,"uids":$userIds}"""
    val hd = JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(payload.noSpaces)
    hd + "." + JwtBase64.encodeString(key.sign(hd.getBytes))

  def createClientToken(): String =
    val tid = UUID.randomUUID().toString
    val exp = Date(System.currentTimeMillis() + 60*60*1000).toString
    val header = s"{\"alg\":\"EdDSA\",\"typ\":\"cjwt\"}"
    val payload = json"""{"app":$id,"tid":$tid,"exp":$exp}"""
    val hd = JwtBase64.encodeString(header) + "." + JwtBase64.encodeString(payload.noSpaces)
    hd + "." + JwtBase64.encodeString(key.sign(hd.getBytes))

  def insert(app: ServerApp): IO[Unit] =
    app.appRepo.insert(models.App(id, key.publicKeyString, "Test App " + id.substring(0, 8)))
}
