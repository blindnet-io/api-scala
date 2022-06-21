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

class CreateDocSpec extends AnyUserAuthEndpointSpec("documents", Method.POST) {
  def payload(testUsers: List[TestUser], aesKey: AesKey): Json =
    testUsers.map(testUser => {
      json"""{
            "userID": ${testUser.id},
            "encryptedSymmetricKey": ${testUser.encKey.encryptToString(json"""{"kty":"oct","k":${aesKey.secretKeyString}}""".noSpaces.getBytes)}
      }"""
    }).asJson

  def createCompleteRequest(testUsers: List[TestUser], aesKey: AesKey, token: String): Request[IO] =
    createAuthedRequest(token).withEntity(payload(testUsers, aesKey))

  override def testUserToken(): IO[Assertion] = {
    val testApp = TestApp()
    val sender = TestUser()
    val testUsers = List.fill(10)(TestUser())
    val token = testApp.createUserToken(sender)
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- sender.insert(serverApp, testApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      res <- run(createCompleteRequest(testUsers, aesKey, token))
      body <- res.as[Json]
      dbDoc <- serverApp.documentRepo.findById(testApp.id, body.asString.get)
      dbKeys <- serverApp.documentKeyRepo.findAllByDocument(testApp.id, dbDoc.get.id)
    } yield {
      assertResult(Status.Ok)(res.status)

      assertResult(testUsers.size)(dbKeys.size)
      testUsers.foreach(testUser => {
        val dbKey = dbKeys.find(key => key.userId == testUser.id)
        assertResult(body.asString.get)(dbKey.get.documentId)
        assertResult(testUser.id)(dbKey.get.userId)
        assertResult(json"""{"kty":"oct","k":${aesKey.secretKeyString}}""".noSpaces.getBytes)(testUser.encKey.decryptFromString(dbKey.get.encSymmetricKey))
      })
      succeed
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
      res <- run(createCompleteRequest(testUsers, aesKey, token))
      body <- res.as[Json]
      dbDoc <- serverApp.documentRepo.findById(testApp.id, body.asString.get)
      dbKeys <- serverApp.documentKeyRepo.findAllByDocument(testApp.id, dbDoc.get.id)
    } yield {
      assertResult(Status.Ok)(res.status)

      assertResult(testUsers.size)(dbKeys.size)
      testUsers.foreach(testUser => {
        val dbKey = dbKeys.find(key => key.userId == testUser.id)
        assertResult(body.asString.get)(dbKey.get.documentId)
        assertResult(testUser.id)(dbKey.get.userId)
        assertResult(json"""{"kty":"oct","k":${aesKey.secretKeyString}}""".noSpaces.getBytes)(testUser.encKey.decryptFromString(dbKey.get.encSymmetricKey))
      })
      succeed
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val group = UUID.randomUUID().toString
    val testUsers = List.fill(10)(TestUser(group = group))
    val token = testApp.createTempUserToken(group)
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      res <- run(createCompleteRequest(testUsers, aesKey, token))
      body <- res.as[Json]
      dbDoc <- serverApp.documentRepo.findById(testApp.id, body.asString.get)
      dbKeys <- serverApp.documentKeyRepo.findAllByDocument(testApp.id, dbDoc.get.id)
    } yield {
      assertResult(Status.Ok)(res.status)

      assertResult(testUsers.size)(dbKeys.size)
      testUsers.foreach(testUser => {
        val dbKey = dbKeys.find(key => key.userId == testUser.id)
        assertResult(body.asString.get)(dbKey.get.documentId)
        assertResult(testUser.id)(dbKey.get.userId)
        assertResult(json"""{"kty":"oct","k":${aesKey.secretKeyString}}""".noSpaces.getBytes)(testUser.encKey.decryptFromString(dbKey.get.encSymmetricKey))
      })
      succeed
    }
  }

  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUsers = List.fill(10)(TestUser())
    val token = testApp.createClientToken()
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createCompleteRequest(testUsers, aesKey, token))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUsers = List.fill(10)(TestUser())
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createRequest().withEntity(payload(testUsers, aesKey)))
    } yield {
      assertResult(Status.Unauthorized)(res.status)
    }
  }

  it("should forbid if temp user cannot encrypt to some user") {
    val testApp = TestApp()
    val testUsers = List.fill(10)(TestUser())
    val token = testApp.createTempUserToken(testUsers.map(_.id).tail)
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUsers.traverse(_.insert(serverApp, testApp))
      res <- run(createCompleteRequest(testUsers, aesKey, token))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  it("should fail if no keys are provided") {
    val testApp = TestApp()
    val token = testApp.createTempUserToken("some_group")
    val aesKey = AesUtil.createKey()

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createCompleteRequest(Nil, aesKey, token))
    } yield {
      assertResult(Status.BadRequest)(res.status)
    }
  }
}
