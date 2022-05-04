package io.blindnet.backend
package func.signaluser

import util.*

import cats.effect.*
import com.dimafeng.testcontainers.ContainerDef
import io.circe.*
import io.circe.literal.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.scalatest.Assertion

import java.util.UUID

class CreateSignalUserSpec extends UserAuthEndpointSpec("signal/users", Method.POST) {
  def payload(testApp: TestApp, testDevice: TestDevice, token: String): Json =
    json"""{
           "deviceID": ${testDevice.id},
           "publicIkID": ${testDevice.ik.id},
           "publicIk": ${testDevice.ik.get.publicKeyString},
           "publicSpkID": ${testDevice.pk.id},
           "publicSpk": ${testDevice.pk.get.publicKeyString},
           "pkSig": ${testDevice.ik.get.signToString(testDevice.pk.get.publicKeyBytes)},
           "signedJwt": ${testDevice.ik.get.signToString(token.getBytes)},
           "signalOneTimeKeys": ${testDevice.otks.map(otk =>
            json"""{
                   "publicOpkID": ${otk.id},
                   "publicOpk": ${otk.get.publicKeyString}
            }""")}
    }"""

  def createCompleteRequest(testApp: TestApp, testDevice: TestDevice, token: String): Request[IO] =
    createAuthedRequest(token).withEntity(payload(testApp, testDevice, token))

  override def testValidRequest(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testDevice = testUser.device()
    val token = testApp.createUserToken(testUser.id, testUser.group)

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createCompleteRequest(testApp, testDevice, token))
      body <- res.as[Json]
      dbUser <- serverApp.userRepo.findById(testApp.id, testUser.id)
      dbDevice <- serverApp.userDeviceRepo.findById(testApp.id, testUser.id, testDevice.id)
      dbOtKeys <- serverApp.oneTimeKeyRepo.findAllByDevice(testApp.id, testUser.id, testDevice.id)
    } yield {
      assertResult(Status.Ok)(res.status)

      assert(body.isString)
      assertResult(testUser.id)(body.asString.get)

      assert(dbUser.isDefined)
      assert(dbDevice.isDefined)
      assertResult(testDevice.ik.id)(dbDevice.get.publicIkId)
      assertResult(testDevice.ik.get.publicKeyString)(dbDevice.get.publicIk)
      assertResult(testDevice.pk.id)(dbDevice.get.publicSpkId)
      assertResult(testDevice.pk.get.publicKeyString)(dbDevice.get.publicSpk)
      assert(testDevice.ik.get.verifyFromString(testDevice.pk.get.publicKeyBytes, dbDevice.get.pkSig))
      assertResult(10)(dbOtKeys.size)
      testDevice.otks.foreach(otKey => {
        val dbOtKey = dbOtKeys.find(_.id == otKey.id).get
        assertResult(otKey.get.publicKeyString)(dbOtKey.key)
      })
      succeed
    }
  }

  override def testNoToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testDevice = testUser.device()
    val token = testApp.createUserToken(testUser.id, testUser.group)

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createRequest().withEntity(payload(testApp, testDevice, token)))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenGid(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testDevice = testUser.device()
    val token = testApp.createTempUserToken(testUser.group)

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createCompleteRequest(testApp, testDevice, token))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testTempUserTokenUids(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testDevice = testUser.device()
    val token = testApp.createTempUserToken(List(testUser.id))

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createCompleteRequest(testApp, testDevice, token))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }

  override def testClientToken(): IO[Assertion] = {
    val testApp = TestApp()
    val testUser = TestUser()
    val testDevice = testUser.device()
    val token = testApp.createClientToken()

    for {
      _ <- testApp.insert(serverApp)
      res <- run(createCompleteRequest(testApp, testDevice, token))
    } yield {
      assertResult(Status.Forbidden)(res.status)
    }
  }
}
