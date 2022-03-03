package io.blindnet.backend
package services

import models.*

import cats.effect.IO
import org.http4s.*
import org.http4s.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*

class Router(userRepo: UserRepository[IO]) {
  private val userService = UserService(userRepo)
  
  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ POST -> Root / "users" => userService.createUser(req)
  }
}
