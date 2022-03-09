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
import org.http4s.server.AuthMiddleware

class UserService(userRepo: UserRepository[IO]) {
  private def authedRoutes = AuthedRoutes.of[UserAuthJwt, IO] {
    case req @ POST -> Root / "users" as jwt => Ok(s"Bonjour ${jwt.userId}")
  }

  private def authMiddleware = AuthMiddleware(AuthJwt.authenticate, Kleisli(req => OptionT.liftF(Forbidden(req.context))))
  def routes: HttpRoutes[IO] = authMiddleware(authedRoutes)
}
