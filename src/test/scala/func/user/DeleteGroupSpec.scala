package io.blindnet.backend
package func.user

import util.*

import cats.effect.*
import cats.implicits.*
import com.dimafeng.testcontainers.ContainerDef
import io.circe.Json
import io.circe.literal.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.implicits.*
import org.scalatest.Assertion

import java.util.UUID

class DeleteGroupSpec extends ClientAuthEndpointSpec("group/%s", Method.DELETE) {
  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUsers = List.fill(10)(TestUser(group = "test_group"))

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      res <- run(createAuthedRequest(testApp.createClientToken(), "test_group"))
      dbUsers <- serverApp.userRepo.findAllByGroup(testApp.id, "test_group")
      dbUser <- serverApp.userRepo.findById(testApp.id, testUser.id)
    } yield {
      assertResult(Status.Ok)(res.status)

      assert(dbUsers.isEmpty)
      assert(dbUser.isDefined)
    }
  }

  override def testUserToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      res <- run(createAuthedRequest(testApp.createUserToken(testUser), "test_group"))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createAuthedRequest(testApp.createTempUserToken("test_group"), "test_group"))
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
      res <- run(createAuthedRequest(testApp.createTempUserToken(List(testUser.id)), "test_group"))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createRequest("test_group"))
    } yield {
      assertResult(Status.Unauthorized)(res.status)
    }
  }
}
