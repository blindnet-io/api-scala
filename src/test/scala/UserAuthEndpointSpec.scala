package io.blindnet.backend

import util.*

import cats.effect.IO
import io.circe.Json
import io.circe.literal.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.implicits.*
import org.scalatest.Assertion

abstract class UserAuthEndpointSpec(path: String, method: Method) extends EndpointSpec(path, method) {
  def createAuthedRequest(token: String, params: String*): Request[IO] = {
    createRequest(params:_*)
      .withHeaders(Headers(("Authorization", "Bearer " + token)))
  }

  def testNoToken(): IO[Assertion]
  def testTempUserTokenGid(): IO[Assertion]
  def testTempUserTokenUids(): IO[Assertion]
  def testClientToken(): IO[Assertion]

  describe("Authentication") {
    it("should forbid no token")(testNoToken())
    it("should forbid temp user tokens with group id")(testTempUserTokenGid())
    it("should forbid temp user tokens with user ids")(testTempUserTokenUids())
    it("should forbid client tokens")(testClientToken())
  }
}
