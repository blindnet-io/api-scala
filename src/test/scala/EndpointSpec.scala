package io.blindnet.backend

import cats.effect.*
import io.circe.Json
import org.http4s.*
import org.scalatest.Assertion

abstract class EndpointSpec(path: String, method: Method) extends FuncSpec {
  def createRequest(): Request[IO] = {
    Request[IO]()
      .withUri(Uri.unsafeFromString("https://test.blindnet.io/api/v1/" + path))
      .withMethod(method)
  }

  def testValidRequest(): IO[Assertion]

  it("should accept a valid request")(testValidRequest())
}
