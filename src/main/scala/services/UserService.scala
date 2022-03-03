package io.blindnet.backend
package services

import models.*

import cats.effect.IO
import org.http4s.*
import org.http4s.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*

class UserService(userRepo: UserRepository[IO]) {
  def createUser(req: Request[IO]): IO[Response[IO]] = ???
}
