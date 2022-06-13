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

class GetDocsKeySpec extends UserAuthEndpointSpec("documents/keys", Method.POST) {
  def payload(docIds: List[String]): Json =
    json"""{
          "data_ids": $docIds
    }"""

  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKeys = List.fill(5)(AesUtil.createKey())
    val aesKeysKeep = List.fill(5)(AesUtil.createKey())

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- aesKeysKeep.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsRes <- aesKeys.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsBodies: List[Json] <- docsRes.traverse(_.as[Json])
      res <- run(createAuthedRequest(testApp.createUserToken(testUser)).withEntity(payload(docsBodies.map(_.asString.get))))
      body <- res.as[Json]
    } yield {
      assertResult(Status.Ok)(res.status)

      val keyArray = body.asArray.get
      aesKeys.zip(docsBodies).foreach {
        case (aesKey, doc) =>
          val keyBody = keyArray.find(_.asObject.get("documentID").get.asString.get == doc.asString.get).get
          val keyJson = parse(String(testUser.encKey.decryptFromString(keyBody.asObject.get("encryptedSymmetricKey").get.asString.get))).getOrElse(throw Exception())
          assertResult(aesKey.secretKeyString)(keyJson.asObject.get("k").get.asString.get)
      }
      succeed
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKeys = List.fill(5)(AesUtil.createKey())
    val aesKeysKeep = List.fill(5)(AesUtil.createKey())

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- aesKeysKeep.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsRes <- aesKeys.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsBodies: List[Json] <- docsRes.traverse(_.as[Json])
      res <- run(createAuthedRequest(testApp.createTempUserToken(testUser.group)).withEntity(payload(docsBodies.map(_.asString.get))))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenUids(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKeys = List.fill(5)(AesUtil.createKey())
    val aesKeysKeep = List.fill(5)(AesUtil.createKey())

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- aesKeysKeep.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsRes <- aesKeys.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsBodies: List[Json] <- docsRes.traverse(_.as[Json])
      res <- run(createAuthedRequest(testApp.createTempUserToken(List(testUser.id))).withEntity(payload(docsBodies.map(_.asString.get))))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKeys = List.fill(5)(AesUtil.createKey())
    val aesKeysKeep = List.fill(5)(AesUtil.createKey())

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- aesKeysKeep.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsRes <- aesKeys.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsBodies: List[Json] <- docsRes.traverse(_.as[Json])
      res <- run(createAuthedRequest(testApp.createClientToken()).withEntity(payload(docsBodies.map(_.asString.get))))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKeys = List.fill(5)(AesUtil.createKey())
    val aesKeysKeep = List.fill(5)(AesUtil.createKey())

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- aesKeysKeep.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsRes <- aesKeys.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsBodies: List[Json] <- docsRes.traverse(_.as[Json])
      res <- run(createRequest().withEntity(payload(docsBodies.map(_.asString.get))))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }
}
