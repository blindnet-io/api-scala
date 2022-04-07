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

class GetUserPublicKeysSpec extends UserAuthEndpointSpec("keys/%s", Method.GET) {
  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUser2 = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUser2.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createUserToken(testUser2), testUser.id))
      body <- res.as[Json]
    } yield {
      assertResult(Status.Ok)(res.status)

      val o = body.asObject.get
      assertResult(testUser.id)(o("userID").get.asString.get)
      assertResult(testUser.encKey.publicKeyString)(o("publicEncryptionKey").get.asString.get)
      assertResult(testUser.sigKey.publicKeyString)(o("publicSigningKey").get.asString.get)
      assertResult(testUser.sigKey.signToString(testUser.encKey.publicKeyBytes))(o("signedPublicEncryptionKey").get.asString.get)
      assertResult(4)(o.size)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createRequest(testUser.id))
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
      res <- run(createAuthedRequest(testApp.createTempUserToken("test_group"), testUser.id))
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
      res <- run(createAuthedRequest(testApp.createTempUserToken(List(testUser.id)), testUser.id))
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
      res <- run(createAuthedRequest(testApp.createClientToken(), testUser.id))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  it("should fail if user does not exist") {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUser2 = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser2.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createUserToken(testUser2), testUser.id))
    } yield {
      assertResult(Status.NotFound)(res.status)
    }
  }

  it("should fail if user belongs to another app") {
    val testApp = TestApp()
    val testApp2 = TestApp()
    val testUser = TestUser()
    val testUser2 = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testApp2.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUser2.insert(serverApp, testApp2)
      res <- run(createAuthedRequest(testApp2.createUserToken(testUser2), testUser.id))
    } yield {
      assertResult(Status.NotFound)(res.status)
    }
  }
}
