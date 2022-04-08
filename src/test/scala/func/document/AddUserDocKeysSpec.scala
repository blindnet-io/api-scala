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

class AddUserDocKeysSpec extends UserAuthEndpointSpec("documents/keys/user/%s", Method.PUT) {
  def payload(docId: String, testUser: TestUser, aesKey: AesKey): Json =
    List(
      json"""{
            "documentID": $docId,
            "encryptedSymmetricKey": ${testUser.encKey.encryptToString(json"""{"kty":"oct","k":${aesKey.secretKeyString}}""".noSpaces.getBytes)}
      }"""
    ).asJson

  def createCompleteRequest(testUser: TestUser, docId: String, aesKey: AesKey, token: String): Request[IO] =
    createAuthedRequest(token, testUser.id).withEntity(payload(docId, testUser, aesKey))

  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUser2 = TestUser()
    val testUsers = List.fill(10)(TestUser())
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUser2.insert(serverApp, testApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      docRes <- run(CreateDocSpec().createCompleteRequest(testUsers, aesKey, testApp.createTempUserToken(testUsers.map(_.id))))
      docBody <- docRes.as[Json]
      res <- run(createCompleteRequest(testUser2, docBody.asString.get, aesKey, testApp.createUserToken(testUser)))
      body <- res.as[Json]
      dbDoc <- serverApp.documentRepo.findById(testApp.id, docBody.asString.get)
      dbKeys <- serverApp.documentKeyRepo.findAllByDocument(testApp.id, dbDoc.get.id)
    } yield {
      assertResult(Status.Ok)(res.status)
      assert(body.isBoolean)
      assert(body.asBoolean.get)

      assertResult(testUsers.size + 1)(dbKeys.size)
      testUsers.foreach(testUser => {
        val dbKey = dbKeys.find(key => key.userId == testUser.id)
        assertResult(docBody.asString.get)(dbKey.get.documentId)
        assertResult(testUser.id)(dbKey.get.userId)
        assertResult(json"""{"kty":"oct","k":${aesKey.secretKeyString}}""".noSpaces.getBytes)(testUser.encKey.decryptFromString(dbKey.get.encSymmetricKey))
      })
      val dbKey = dbKeys.find(key => key.userId == testUser2.id)
      assertResult(docBody.asString.get)(dbKey.get.documentId)
      assertResult(testUser2.id)(dbKey.get.userId)
      assertResult(json"""{"kty":"oct","k":${aesKey.secretKeyString}}""".noSpaces.getBytes)(testUser2.encKey.decryptFromString(dbKey.get.encSymmetricKey))
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser2 = TestUser()
    val testUsers = List.fill(10)(TestUser())
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser2.insert(serverApp, testApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      docRes <- run(CreateDocSpec().createCompleteRequest(testUsers, aesKey, testApp.createTempUserToken(testUsers.map(_.id))))
      docBody <- docRes.as[Json]
      res <- run(createCompleteRequest(testUser2, docBody.asString.get, aesKey, testApp.createTempUserToken("some_group")))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenUids(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser2 = TestUser()
    val testUsers = List.fill(10)(TestUser())
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser2.insert(serverApp, testApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      docRes <- run(CreateDocSpec().createCompleteRequest(testUsers, aesKey, testApp.createTempUserToken(testUsers.map(_.id))))
      docBody <- docRes.as[Json]
      res <- run(createCompleteRequest(testUser2, docBody.asString.get, aesKey, testApp.createTempUserToken(List(testUser2.id))))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser2 = TestUser()
    val testUsers = List.fill(10)(TestUser())
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser2.insert(serverApp, testApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      docRes <- run(CreateDocSpec().createCompleteRequest(testUsers, aesKey, testApp.createTempUserToken(testUsers.map(_.id))))
      docBody <- docRes.as[Json]
      res <- run(createCompleteRequest(testUser2, docBody.asString.get, aesKey, testApp.createClientToken()))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testUser2 = TestUser()
    val testUsers = List.fill(10)(TestUser())
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- testUser2.insert(serverApp, testApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      docRes <- run(CreateDocSpec().createCompleteRequest(testUsers, aesKey, testApp.createTempUserToken(testUsers.map(_.id))))
      docBody <- docRes.as[Json]
      res <- run(createRequest(testUser2.id).withEntity(payload(docBody.asString.get, testUser2, aesKey)))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }
}
