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

class GetAllDocsKeySpec extends UserAuthEndpointSpec("documents/keys", Method.GET) {
  override def testUserToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKeys = List.fill(10)(AesUtil.createKey())

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      docsRes <- aesKeys.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      docsBodies: List[Json] <- docsRes.traverse(_.as[Json])
      res <- run(createAuthedRequest(testApp.createUserToken(testUser)))
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
    val testUser = TestUser(group = "test_group")
    val aesKeys = List.fill(10)(AesUtil.createKey())

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- aesKeys.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      res <- run(createAuthedRequest(testApp.createTempUserToken("test_group")))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenUids(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKeys = List.fill(10)(AesUtil.createKey())

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- aesKeys.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      res <- run(createAuthedRequest(testApp.createTempUserToken(List(testUser.id))))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKeys = List.fill(10)(AesUtil.createKey())

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- aesKeys.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      res <- run(createAuthedRequest(testApp.createClientToken()))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val aesKeys = List.fill(10)(AesUtil.createKey())

    for {
      _ <- testApp.insert(serverApp)
      _ <- testUser.insert(serverApp, testApp)
      _ <- aesKeys.traverse(key => run(CreateDocSpec().createCompleteRequest(List(testUser), key, testApp.createTempUserToken(List(testUser.id)))))
      res <- run(createRequest())
    } yield {
      assertResult(Status.Unauthorized)(res.status)
    }
  }
}
