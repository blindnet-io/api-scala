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

class CreateUserSpec extends UserAuthEndpointSpec("users", Method.POST) {
  def payload(testApp: TestApp, testUser: TestUser, token: String): Json =
    json"""{
           "publicEncryptionKey": ${testUser.encKey.publicKeyString},
           "publicSigningKey": ${testUser.sigKey.publicKeyString},
           "signedJwt": ${testUser.sigKey.signToString(token.getBytes)},
           "encryptedPrivateEncryptionKey": ${testUser.aes.encryptToString(testUser.encKey.privateKeyBytes)},
           "encryptedPrivateSigningKey": ${testUser.aes.encryptToString(testUser.sigKey.privateKeyBytes)},
           "keyDerivationSalt": ${testUser.saltString},
           "signedPublicEncryptionKey": ${testUser.sigKey.signToString(testUser.encKey.publicKeyBytes)}
    }"""

  def createCompleteRequest(testApp: TestApp, testUser: TestUser, token: String): Request[IO] =
    createAuthedRequest(token).withEntity(payload(testApp, testUser, token))

  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createUserToken(testUser.id, testUser.group)

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createCompleteRequest(testApp, testUser, token))
      body <- res.as[Json]
    } yield {
      assertResult(Status.Ok)(res.status)

      assert(body.isString)
      assertResult(testUser.id)(body.asString.get)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createUserToken(testUser.id, testUser.group)

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createRequest().withEntity(payload(testApp, testUser, token)))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createTempUserToken(testUser.group)

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createCompleteRequest(testApp, testUser, token))
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
      res <- run(createCompleteRequest(testApp, testUser, token))
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
      res <- run(createCompleteRequest(testApp, testUser, token))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  it("should fail if user already exists") {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createUserToken(testUser.id, testUser.group)

    for {
      _ <- testApp.insert(serverApp)
      _ <- run(createCompleteRequest(testApp, testUser, token))
        .asserting(res => assertResult(Status.Ok)(res.status))
      res <- run(createCompleteRequest(testApp, testUser, token))
    } yield {
      assertResult(Status.BadRequest)(res.status)
    }
  }

  it("should forbid bad JWT signature") {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createUserToken(testUser.id, testUser.group)

    val badPayload = payload(testApp, testUser, token).asObject.get.add("signedJwt", "badsign".asJson)

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createAuthedRequest(token).withEntity(badPayload.asJson))
      body <- res.as[String]
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  it("should forbid bad PK signature") {
    val testApp = TestApp()
    val testUser = TestUser()
    val token = testApp.createUserToken(testUser.id, testUser.group)

    val badPayload = payload(testApp, testUser, token).asObject.get.add("signedPublicEncryptionKey", "badsign".asJson)

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createAuthedRequest(token).withEntity(badPayload.asJson))
      body <- res.as[String]
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }
}
