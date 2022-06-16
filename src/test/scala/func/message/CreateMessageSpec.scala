package io.blindnet.backend
package func.message

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
import org.scalatest.{Assertion, Ignore}

import java.time.Instant
import java.util.UUID

@Ignore
class CreateMessageSpec extends UserAuthEndpointSpec("messages", Method.POST) {
  def payload(recipientId: String, message: String, timestamp: String): Json =
    json"""{
          "recipientID": $recipientId,
          "message": $message,
          "timestamp": $timestamp
    }"""

  def createValidRequest(token: String, recipient: TestUser, message: String, timestamp: Instant): Request[IO] =
    createAuthedRequest(token).withEntity(payload(recipient.id, message, timestamp.toString))

  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val sender = TestUser()
    val recipient = TestUser()
    val timeSent = Instant.now()

    for {
      _ <- testApp.insert(serverApp)
      _ <- sender.insert(serverApp, testApp)
      _ <- recipient.insert(serverApp, testApp)
      res <- run(createValidRequest(testApp.createUserToken(sender), recipient, "test message", timeSent))
      msgId <- res.as[Json].map(_.asString.get.toLong)
      dbMsg <- serverApp.messageRepo.findById(testApp.id, msgId)
    } yield {
      assertResult(Status.Ok)(res.status)

      assert(dbMsg.isDefined)
      assertResult(sender.id)(dbMsg.get.senderId)
      assertResult(recipient.id)(dbMsg.get.recipientId)
      assertResult(timeSent.toEpochMilli)(dbMsg.get.timeSent.toEpochMilli)
      assertResult("test message")(dbMsg.get.data)
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val sender = TestUser()
    val recipient = TestUser()
    val timeSent = Instant.now()

    for {
      _ <- testApp.insert(serverApp)
      _ <- sender.insert(serverApp, testApp)
      _ <- recipient.insert(serverApp, testApp)
      res <- run(createValidRequest(testApp.createTempUserToken(recipient.group), recipient, "test message", timeSent))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenUids(): IO[Assertion] = {
    val testApp = TestApp()
    val sender = TestUser()
    val recipient = TestUser()
    val timeSent = Instant.now()

    for {
      _ <- testApp.insert(serverApp)
      _ <- sender.insert(serverApp, testApp)
      _ <- recipient.insert(serverApp, testApp)
      res <- run(createValidRequest(testApp.createTempUserToken(List(recipient.id)), recipient, "test message", timeSent))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val sender = TestUser()
    val recipient = TestUser()
    val timeSent = Instant.now()

    for {
      _ <- testApp.insert(serverApp)
      _ <- sender.insert(serverApp, testApp)
      _ <- recipient.insert(serverApp, testApp)
      res <- run(createValidRequest(testApp.createClientToken(), recipient, "test message", timeSent))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val sender = TestUser()
    val recipient = TestUser()
    val timeSent = Instant.now()

    for {
      _ <- testApp.insert(serverApp)
      _ <- sender.insert(serverApp, testApp)
      _ <- recipient.insert(serverApp, testApp)
      res <- run(createRequest().withEntity(payload(recipient.id, "test message", timeSent.toString)))
    } yield {
      assertResult(Status.Unauthorized)(res.status)
    }
  }
}
