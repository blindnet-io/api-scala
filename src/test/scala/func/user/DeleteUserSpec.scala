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

class DeleteUserSpec extends ClientAuthEndpointSpec("users/%s", Method.DELETE) {
  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createClientToken(), testUser.id))
      dbUser <- serverApp.userRepo.findById(testApp.id, testUser.id)
    } yield {
      assertResult(Status.Ok)(res.status)

      assert(dbUser.isEmpty)
    }
  }

  override def testUserToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createUserToken(testUser), testUser.id))
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

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createRequest(testUser.id))
    } yield {
      assertResult(Status.Unauthorized)(res.status)
    }
  }

  it("should fail if user belongs to another app") {
    val testApp = TestApp()
    val testApp2 = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testApp2.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp2.createClientToken(), testUser.id))
    } yield {
      assertResult(Status.NotFound)(res.status)
    }
  }

  it("should fail if user does not exist") {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createAuthedRequest(testApp.createClientToken(), testUser.id))
    } yield {
      assertResult(Status.NotFound)(res.status)
    }
  }
}
