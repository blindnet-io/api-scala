package io.blindnet.backend

import cats.effect.IO
import io.circe.Json
import io.circe.literal.*
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.implicits.*
import org.scalatest.Assertion

abstract class TempUserAuthEndpointSpec(path: String, method: Method) extends AnyUserAuthEndpointSpec(path, method) {
  def testUserToken(): IO[Assertion]

  describe("Authentication") {
    it("should forbid user tokens")(testUserToken())
  }
}
