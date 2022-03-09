package io.blindnet.backend
package services

import auth.*
import models.*

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.*

class ServicesRouter(userRepo: UserRepository[IO]) {
  private val userService = UserService(userRepo)

  def routes: HttpRoutes[IO] = userService.routes
}
