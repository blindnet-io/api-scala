package io.blindnet.backend
package services

import auth.*
import models.*

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.implicits.*
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
import org.http4s.server.middleware.*

class ServicesRouter(userRepo: UserRepository[IO], documentRepo: DocumentRepository[IO], documentKeyRepo: DocumentKeyRepository[IO]) {
  private val userService = UserService(userRepo)
  private val documentService = DocumentService(documentRepo, documentKeyRepo)

  def routes: HttpRoutes[IO] = userService.routes <+> documentService.routes
  def corsRoutes: HttpRoutes[IO] = CORS.policy.withAllowOriginAll(routes)
}
