package io.blindnet.backend

import cats.effect.IO
import io.circe.Json
import io.circe.literal.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.implicits.*
import org.scalatest.Assertion

abstract class ClientAuthEndpointSpec(path: String, method: Method) extends AuthEndpointSpec(path, method) {
  def testUserToken(): IO[Assertion]
  def testTempUserTokenGid(): IO[Assertion]
  def testTempUserTokenUids(): IO[Assertion]

  describe("Authentication") {
    it("should forbid user tokens")(testUserToken())
    it("should forbid temp user tokens with group id")(testTempUserTokenGid())
    it("should forbid temp user tokens with user ids")(testTempUserTokenUids())
  }
}
