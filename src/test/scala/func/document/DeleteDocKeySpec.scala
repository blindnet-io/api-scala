package io.blindnet.backend
package func.document

import util.*

import cats.effect.*
import cats.implicits.*
import com.dimafeng.testcontainers.ContainerDef
import io.circe.*
import io.circe.literal.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.scalatest.Assertion

import java.util.UUID

class DeleteDocKeySpec extends ClientAuthEndpointSpec("documents/%s/keys/%s", Method.DELETE) {
  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUserKeep = TestUser()
    val token = testApp.createTempUserToken(List(testUser.id, testUserKeep.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUserKeep.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUser, testUserKeep), aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createAuthedRequest(testApp.createClientToken(), docId, testUser.id))
      dbDoc <- serverApp.documentRepo.findById(testApp.id, docId)
      dbKeys <- serverApp.documentKeyRepo.findAllByDocument(testApp.id, docId)
    } yield {
      assertResult(Status.Ok)(res.status)

      assert(dbDoc.isDefined)
      assertResult(docId)(dbDoc.get.id)

      assertResult(1)(dbKeys.size)
      assertResult(testUserKeep.id)(dbKeys.head.userId)
    }
  }

  override def testUserToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUserKeep = TestUser()
    val token = testApp.createTempUserToken(List(testUser.id, testUserKeep.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUserKeep.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUser, testUserKeep), aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createAuthedRequest(testApp.createUserToken(testUser), docId, testUser.id))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUserKeep = TestUser()
    val token = testApp.createTempUserToken(List(testUser.id, testUserKeep.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUserKeep.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUser, testUserKeep), aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createAuthedRequest(testApp.createTempUserToken(testUser.group), docId, testUser.id))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenUids(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUserKeep = TestUser()
    val token = testApp.createTempUserToken(List(testUser.id, testUserKeep.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUserKeep.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUser, testUserKeep), aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createAuthedRequest(testApp.createTempUserToken(List(testUser.id)), docId, testUser.id))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUserKeep = TestUser()
    val token = testApp.createTempUserToken(List(testUser.id, testUserKeep.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUserKeep.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUser, testUserKeep), aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createRequest(docId, testUser.id))
    } yield {
      assertResult(Status.Unauthorized)(res.status)
    }
  }

  it("should fail if document does not exist") {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUserKeep = TestUser()
    val token = testApp.createTempUserToken(List(testUser.id, testUserKeep.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUserKeep.insert(serverApp, testApp)
      _ <- run(CreateDocSpec().createCompleteRequest(List(testUser, testUserKeep), aesKey, token))
      res <- run(createAuthedRequest(testApp.createClientToken(), UUID.randomUUID().toString, testUser.id))
    } yield {
      assertResult(Status.NotFound)(res.status)
    }
  }

  it("should fail if user key does not exist") {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUserKeep = TestUser()
    val token = testApp.createTempUserToken(List(testUser.id, testUserKeep.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUserKeep.insert(serverApp, testApp)
      docRes <- run(CreateDocSpec().createCompleteRequest(List(testUserKeep), aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createAuthedRequest(testApp.createClientToken(), docId, testUser.id))
    } yield {
      assertResult(Status.NotFound)(res.status)
    }
  }
}
