package io.blindnet.backend
package services

import auth.*
import models.*

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import org.http4s.*
import org.http4s.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.*

class ServicesRouter(userRepo: UserRepository[IO]) {
  private val userService = UserService(userRepo)

  private def authedRoutes = AuthedRoutes.of[AuthJwt, IO] {
    case req @ GET -> Root / "users" as jwt => Ok(s"Bonjour ${jwt}") 
//    case req @ POST -> Root / "users" as jwt => Ok(s"Bonjour ${jwt}") 
//    case req @ PUT -> Root / "keys" / "me" => userService.updateSelfUser(req)
//    case req @ GET -> Root / "keys" / "me" => userService.getSelfUser(req)
//    case req @ GET -> Root / "keys" / id => userService.getUser(req, id)
//    case req @ POST -> Root / "keys" => userService.getUsers(req)
//    case req @ DELETE -> Root / "users" / id => userService.deleteUser(req, id)
//    case req @ DELETE -> Root / "users" / "me" => userService.deleteSelfUser(req)
//    case req @ DELETE -> Root / "group" / id => userService.deleteGroup(req, id)
  }

  private def authMiddleware = AuthMiddleware(AuthJwt.authenticate, Kleisli(req => OptionT.liftF(Forbidden())))

  def routes: HttpRoutes[IO] = authMiddleware(authedRoutes)
}
