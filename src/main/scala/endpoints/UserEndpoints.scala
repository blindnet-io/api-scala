package io.blindnet.backend
package endpoints

import auth.JwtAuthenticator
import objects.*
import services.UserService

import cats.effect.IO
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.*
import sttp.tapir.server.http4s.*

class UserEndpoints(auth: JwtAuthenticator, service: UserService) {
  private val base = auth.secureEndpoint.tag("Users")

  val createUser: ApiEndpoint =
    base.summary("Create User (FR-BE01)")
      .post
      .in("users")
      .in(header[Option[String]]("Authorization"))
      .in(jsonBody[CreateUserPayload])
      .out(jsonBody[String])
      .serverLogicSuccess(service.createUser)

  val getSelfKeys: ApiEndpoint =
    base.summary("Get Self Keys (FR-BE02)")
      .get
      .in("keys" / "me")
      .out(jsonBody[UserKeysResponse])
      .serverLogicSuccess(service.getSelfKeys)

  val getUserPublicKeys: ApiEndpoint =
    base.summary("Get User Public Keys (FR-BE03)")
      .get
      .in("keys" / path[String]("userId"))
      .out(jsonBody[UserPublicKeysResponse])
      .serverLogicSuccess(service.getUserPublicKeys)

  val getUsersPublicKeys: ApiEndpoint =
    base.summary("Get Users Public Keys (FR-BE04 FR-BE05)")
      .post
      .in("keys")
      .in(jsonBody[UsersPublicKeysPayload])
      .out(jsonBody[List[UserPublicKeysResponse]])
      .serverLogicSuccess(service.getUsersPublicKeys)

  val updatePrivateKeys: ApiEndpoint =
    base.summary("Update Private Keys (FR-BE09)")
      .put
      .in("keys" / "me")
      .in(jsonBody[UpdateUserPrivateKeysPayload])
      .serverLogicSuccess(service.updatePrivateKeys)

  val deleteSelfUser: ApiEndpoint =
    base.summary("Delete Self User (FR-UM04)")
      .delete
      .in("users" / "me")
      .serverLogicSuccess(service.deleteSelfUser)

  val deleteUser: ApiEndpoint =
    base.summary("Delete User (FR-BE13)")
      .delete
      .in("users" / path[String]("userId"))
      .serverLogicSuccess(service.deleteUser)

  val deleteGroup: ApiEndpoint =
    base.summary("Delete Group (FR-BE14)")
      .delete
      .in("group" / path[String]("groupId"))
      .serverLogicSuccess(service.deleteGroup)

  val list: List[ApiEndpoint] = List(
    createUser,
    getSelfKeys,
    getUserPublicKeys,
    getUsersPublicKeys,
    updatePrivateKeys,
    deleteSelfUser,
    deleteUser,
    deleteGroup
  )
}
