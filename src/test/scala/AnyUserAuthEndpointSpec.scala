package io.blindnet.backend

import cats.effect.IO
import io.circe.Json
import io.circe.literal.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.implicits.*
import org.scalatest.Assertion

abstract class AnyUserAuthEndpointSpec(path: String, method: Method) extends AuthEndpointSpec(path, method) {
  def testValidRequest(): IO[Assertion] = IO.pure(succeed)
  
  it("should accept a valid request with user token")(testUserToken())
  it("should accept a valid request with temp UID token")(testTempUserTokenUids())
  it("should accept a valid request with temp GID token")(testTempUserTokenGid())
  
  describe("Authentication") {
    it("should forbid client tokens")(testClientToken())
  }
}
