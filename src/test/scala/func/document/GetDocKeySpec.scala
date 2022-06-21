package io.blindnet.backend
package func.document

import util.*

import cats.effect.*
import cats.implicits.*
import com.dimafeng.testcontainers.ContainerDef
import io.circe.*
import io.circe.literal.*
import io.circe.parser.parse
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.scalatest.Assertion

class GetDocKeySpec extends UserAuthEndpointSpec("documents/keys/%s", Method.GET) {
  override def testUserToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUser), aesKey, testApp.createTempUserToken(List(testUser.id))))
      docBody <- docRes.as[Json]
      res <- run(createAuthedRequest(testApp.createUserToken(testUser), docBody.asString.get))
      body <- res.as[Json]
    } yield {
      assertResult(Status.Ok)(res.status)

      val keyJson = parse(String(testUser.encKey.decryptFromString(body.asString.get))).getOrElse(throw Exception())
      assertResult(aesKey.secretKeyString)(keyJson.asObject.get("k").get.asString.get)
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser(group = "test_group")
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUser), aesKey, testApp.createTempUserToken(List(testUser.id))))
      docBody <- docRes.as[Json]
      res <- run(createAuthedRequest(testApp.createTempUserToken("test_group"), docBody.asString.get))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenUids(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUser), aesKey, testApp.createTempUserToken(List(testUser.id))))
      docBody <- docRes.as[Json]
      res <- run(createAuthedRequest(testApp.createTempUserToken(List(testUser.id)), docBody.asString.get))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUser), aesKey, testApp.createTempUserToken(List(testUser.id))))
      docBody <- docRes.as[Json]
      res <- run(createAuthedRequest(testApp.createClientToken(), docBody.asString.get))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUser), aesKey, testApp.createTempUserToken(List(testUser.id))))
      docBody <- docRes.as[Json]
      res <- run(createRequest(docBody.asString.get))
    } yield {
      assertResult(Status.Unauthorized)(res.status)
    }
  }
}
