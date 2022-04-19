package io.blindnet.backend
package func.user

import util.*

import cats.effect.*
import com.dimafeng.testcontainers.ContainerDef
import io.circe.*
import io.circe.literal.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.scalatest.Assertion

import java.util.UUID

class GetSelfKeysSpec extends UserAuthEndpointSpec("keys/me", Method.GET) {
  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createUserToken(testUser.id, "test_group")

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(token))
      body <- res.as[Json]
    } yield {
      assertResult(Status.Ok)(res.status)

      val o = body.asObject.get
      assertResult(testUser.id)(o("userID").get.asString.get)
      assertResult(testUser.encKey.publicKeyString)(o("publicEncryptionKey").get.asString.get)
      assertResult(testUser.sigKey.publicKeyString)(o("publicSigningKey").get.asString.get)
      assertResult(testUser.sigKey.signToString(testUser.encKey.publicKeyBytes))(o("signedPublicEncryptionKey").get.asString.get)
      assertResult(testUser.encKey.privateKeyBytes)(testUser.aes.decryptFromString(o("encryptedPrivateEncryptionKey").get.asString.get))
      assertResult(testUser.sigKey.privateKeyBytes)(testUser.aes.decryptFromString(o("encryptedPrivateSigningKey").get.asString.get))
      assertResult(testUser.saltString)(o("keyDerivationSalt").get.asString.get)
      assertResult(7)(o.size)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createRequest())
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createTempUserToken("test_group")

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(token))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenUids(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createTempUserToken(List(testUser.id))

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(token))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createClientToken()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(token))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  it("should fail if user does not exist") {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createUserToken(testUser.id, "test_group")

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createAuthedRequest(token))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }
}
