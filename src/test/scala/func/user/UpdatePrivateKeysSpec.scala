package io.blindnet.backend
package func.user

import util.*

import cats.effect.*
import com.dimafeng.testcontainers.ContainerDef
import io.circe.Json
import io.circe.literal.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.implicits.*
import org.scalatest.Assertion

import java.util.UUID

class UpdatePrivateKeysSpec extends UserAuthEndpointSpec("keys/me", Method.PUT) {
  def payload(testUser: TestUser): Json =
    json"""{
           "encryptedPrivateEncryptionKey": ${testUser.aes.encryptToString(testUser.encKey.privateKeyBytes)},
           "encryptedPrivateSigningKey": ${testUser.aes.encryptToString(testUser.sigKey.privateKeyBytes)},
           "keyDerivationSalt": ${testUser.saltString}
    }"""

  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val newUser = testUser.changePasswordAndSalt()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createUserToken(newUser))
        .withEntity(payload(newUser)))
      dbUser <- serverApp.userRepo.findById(testApp.id, newUser.id)
      dbUserKeys <- serverApp.userKeysRepo.findById(testApp.id, newUser.id)
    } yield {
      assertResult(Status.Ok)(res.status)

      assert(dbUser.isDefined)
      assert(dbUserKeys.isDefined)
      assertResult(newUser.encKey.privateKeyBytes)(newUser.aes.decryptFromString(dbUserKeys.get.encPrivateEncKey))
      assertResult(newUser.sigKey.privateKeyBytes)(newUser.aes.decryptFromString(dbUserKeys.get.encPrivateSignKey))
      assertResult(newUser.saltString)(dbUserKeys.get.keyDerivationSalt)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createRequest()
        .withEntity(payload(testUser)))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createTempUserToken("test_group"))
        .withEntity(payload(testUser)))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenUids(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createTempUserToken(List(testUser.id)))
        .withEntity(payload(testUser)))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createClientToken())
        .withEntity(payload(testUser)))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  it("should fail if user does not exist") {
    val testApp = TestApp()
    val testUser = TestUser()
    val newUser = testUser.changePasswordAndSalt()

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createAuthedRequest(testApp.createUserToken(newUser))
        .withEntity(payload(newUser)))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }
}
