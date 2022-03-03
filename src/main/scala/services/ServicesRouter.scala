package io.blindnet.backend
package services

import models.*

import cats.effect.IO
import org.http4s.*
import org.http4s.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*

class ServicesRouter(userRepo: UserRepository[IO]) {
  private val userService = UserService(userRepo)

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "users" => userService.createUser(req)
    case req @ PUT -> Root / "keys" / "me" => userService.updateSelfUser(req)
    case req @ GET -> Root / "keys" / "me" => userService.getSelfUser(req)
    case req @ GET -> Root / "keys" / id => userService.getUser(req, id)
    case req @ POST -> Root / "keys" => userService.getUsers(req)
    case req @ DELETE -> Root / "users" / id => userService.deleteUser(req, id)
    case req @ DELETE -> Root / "users" / "me" => userService.deleteSelfUser(req)
    case req @ DELETE -> Root / "group" / id => userService.deleteGroup(req, id)
  }
}
