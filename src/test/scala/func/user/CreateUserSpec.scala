package io.blindnet.backend
package func.user

import util.*

import cats.effect.*
import com.dimafeng.testcontainers.ContainerDef
import io.circe.*
import io.circe.literal.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.scalatest.Assertion

class CreateUserSpec extends UserAuthEndpointSpec("users", Method.POST) {
  def payload(testApp: TestApp, testUser: TestUser, token: String): Json =
    json"""{
           "publicEncryptionKey": ${testUser.encKey.publicKeyString},
           "publicSigningKey": ${testUser.sigKey.publicKeyString},
           "signedJwt": ${testUser.sigKey.signToString(token.getBytes)},
           "encryptedPrivateEncryptionKey": ${testUser.aes.encryptToString(testUser.encKey.privateKeyBytes)},
           "encryptedPrivateSigningKey": ${testUser.aes.encryptToString(testUser.sigKey.privateKeyBytes)},
           "keyDerivationSalt": ${testUser.aes.saltString},
           "signedPublicEncryptionKey": ${testUser.sigKey.signToString(testUser.encKey.publicKeyBytes)}
    }"""

  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createUserToken(testApp.id, testUser.id, "test_group")

    val req = createAuthedRequest(token).withEntity(payload(testApp, testUser, token))

    for {
      _ <- testApp.insert(serverApp)
      res <- run(req)
      body <- res.as[String]
    } yield {
      assertResult(Status.Ok)(res.status)
      assertResult(testUser.id)(body)
    }
  }
}
