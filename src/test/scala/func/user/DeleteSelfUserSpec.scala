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

class DeleteSelfUserSpec extends UserAuthEndpointSpec("users/me", Method.DELETE) {
  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createUserToken(testUser)))
      dbUser <- serverApp.userRepo.findById(testApp.id, testUser.id)
    } yield {
      assertResult(Status.Ok)(res.status)

      assert(dbUser.isEmpty)
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createTempUserToken(testUser.group)))
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
      res <- run(createAuthedRequest(testApp.createTempUserToken(List(testUser.id))))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createAuthedRequest(testApp.createClientToken()))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    for {
      res <- run(createRequest())
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  it("should fail if user does not exist") {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createAuthedRequest(testApp.createUserToken(testUser)))
    } yield {
      assertResult(Status.BadRequest)(res.status)
    }
  }
}
