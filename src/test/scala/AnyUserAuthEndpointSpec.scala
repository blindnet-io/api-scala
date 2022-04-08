package io.blindnet.backend

import cats.effect.IO
import io.circe.Json
import io.circe.literal.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.implicits.*
import org.scalatest.Assertion

abstract class AnyUserAuthEndpointSpec(path: String, method: Method) extends AuthEndpointSpec(path, method) {
  def testClientToken(): IO[Assertion]

  describe("Authentication") {
    it("should forbid client tokens")(testClientToken())
  }
}
