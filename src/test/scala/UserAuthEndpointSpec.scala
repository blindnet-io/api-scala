package io.blindnet.backend

import util.*

import cats.effect.IO
import io.circe.Json
import io.circe.literal.*
import org.http4s.*
import org.http4s.implicits.*
import org.scalatest.Assertion

abstract class UserAuthEndpointSpec(path: String, method: Method) extends EndpointSpec(path, method) {
  def createAuthedRequest(token: String): Request[IO] = {
    createRequest()
      .withHeaders(Headers(("Authorization", "Bearer " + token)))
  }
}
