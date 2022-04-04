package io.blindnet.backend
package func

import util.*

import cats.effect.*
import com.dimafeng.testcontainers.ContainerDef
import io.circe.literal.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.implicits.*

class UserFuncSpec extends FuncSpec {
  describe("CreateUser endpoint") {
    it("should register a given user") {
      val app = createServerApp

      val testApp = TestApp()
      val user = TestUser()
      val token = testApp.createUserToken(testApp.id, user.id, "test_group")

      for {
        _ <- testApp.insert(app)
        res <- run(app, request(token)
          .withUri(uri"https://test.blindnet.io/api/v1/users")
          .withMethod(Method.POST)
          .withEntity(
            json"""{
               "publicEncryptionKey": ${user.encKey.publicKeyString},
               "publicSigningKey": ${user.sigKey.publicKeyString},
               "signedJwt": ${user.sigKey.signToString(token.getBytes)},
               "encryptedPrivateEncryptionKey": ${user.aes.encryptToString(user.encKey.privateKeyBytes)},
               "encryptedPrivateSigningKey": ${user.aes.encryptToString(user.sigKey.privateKeyBytes)},
               "keyDerivationSalt": ${user.aes.saltString},
               "signedPublicEncryptionKey": ${user.sigKey.signToString(user.encKey.publicKeyBytes)}
                }"""))
      } yield assertResult(Status.Ok)(res.status)
    }
  }
}
