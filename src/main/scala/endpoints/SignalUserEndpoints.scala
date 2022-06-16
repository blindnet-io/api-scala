package io.blindnet.backend
package endpoints

import auth.JwtAuthenticator
import objects.*
import services.SignalUserService

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*

class SignalUserEndpoints(auth: JwtAuthenticator, service: SignalUserService) {
  private val base = auth.secureEndpoint.tag("Signal Users")

  val createUser: ServerEndpoint[Any, IO] =
    base.summary("Create User (FR-UM01)")
      .post
      .in("signal" / "users")
      .in(TapirAuth.bearer[String]())
      .in(jsonBody[CreateSignalUserPayload])
      .out(jsonBody[String])
      .serverLogicSuccess(service.createUser)

  val updateUser: ServerEndpoint[Any, IO] =
    base.summary("Update User (FR-UM02)")
      .put
      .in("signal" / "keys" / "me")
      .in(jsonBody[UpdateSignalUserPayload])
      .out(jsonBody[Boolean])
      .serverLogicSuccess(service.updateUser)

  val getUserKeys: ServerEndpoint[Any, IO] =
    base.summary("Get User Keys (FR-UM03)")
      .get
      .in("signal" / "keys" / path[String]("userId"))
      .in(query[List[String]]("deviceID"))
      .out(jsonBody[List[UserKeysResponseItem]])
      .serverLogicSuccess(service.getUserKeys)

  val getUserDevices: ServerEndpoint[Any, IO] =
    base.summary("Get User Devices")
      .get
      .in("signal" / "devices" / path[String]("userId"))
      .out(jsonBody[List[UserDevicesResponseItem]])
      .serverLogicSuccess(service.getUserDevices)

  val list: List[ServerEndpoint[Any, IO]] = List(
    createUser,
    updateUser,
    getUserKeys,
    getUserDevices,
  )
}
