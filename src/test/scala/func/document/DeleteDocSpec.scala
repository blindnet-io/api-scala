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

class DeleteDocSpec extends ClientAuthEndpointSpec("documents/%s", Method.DELETE) {
  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUsers = List.fill(10)(TestUser())
    val token = testApp.createTempUserToken(testUsers.map(_.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      docRes <- run(CreateDocSpec().createCompleteRequest(testUsers, aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createAuthedRequest(testApp.createClientToken(), docId))
      dbDoc <- serverApp.documentRepo.findById(testApp.id, docId)
      dbKeys <- serverApp.documentKeyRepo.findAllByDocument(testApp.id, docId)
    } yield {
      assertResult(Status.Ok)(res.status)

      assert(dbDoc.isEmpty)
      assert(dbKeys.isEmpty)
    }
  }

  override def testUserToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUsers = List.fill(10)(TestUser())
    val token = testApp.createTempUserToken(testUsers.map(_.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      docRes <- run(CreateDocSpec().createCompleteRequest(testUsers, aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createAuthedRequest(testApp.createUserToken(testUsers.head), docId))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val testUsers = List.fill(10)(TestUser())
    val token = testApp.createTempUserToken(testUsers.map(_.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      docRes <- run(CreateDocSpec().createCompleteRequest(testUsers, aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createAuthedRequest(testApp.createTempUserToken(testUsers.head.group), docId))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenUids(): IO[Assertion] = {
    val testApp = TestApp()
    val testUsers = List.fill(10)(TestUser())
    val token = testApp.createTempUserToken(testUsers.map(_.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      docRes <- run(CreateDocSpec().createCompleteRequest(testUsers, aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createAuthedRequest(testApp.createTempUserToken(testUsers.map(_.id)), docId))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUsers = List.fill(10)(TestUser())
    val token = testApp.createTempUserToken(testUsers.map(_.id))
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      docRes <- run(CreateDocSpec().createCompleteRequest(testUsers, aesKey, token))
      docId <- docRes.as[Json].map(_.asString.get)
      res <- run(createRequest(docId))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }
}
