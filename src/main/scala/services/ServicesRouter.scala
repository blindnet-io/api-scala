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

class ServicesRouter(
    appRepo: AppRepository[IO],
    userRepo: UserRepository[IO],
    userKeysRepo: UserKeysRepository[IO],
    deviceRepo: UserDeviceRepository[IO],
    oneTimeKeyRepo: OneTimeKeyRepository[IO],
    documentRepo: DocumentRepository[IO],
    documentKeyRepo: DocumentKeyRepository[IO],
    messageRepo: MessageRepository[IO]) {
  private val userService = UserService(userRepo, userKeysRepo)
  private val signalUserService = SignalUserService(userRepo, deviceRepo, oneTimeKeyRepo)
  private val documentService = DocumentService(userRepo, documentRepo, documentKeyRepo)
  private val messageService = MessageService(userRepo, deviceRepo, messageRepo)

  private val authenticator = JwtAuthenticator(appRepo, userRepo)

  private def authedRoutes =
    userService.authedRoutes
    <+> signalUserService.authedRoutes
    <+> documentService.authedRoutes
    <+> messageService.authedRoutes

  private def routes: HttpRoutes[IO] = authenticator.authMiddleware(authedRoutes)
  def corsRoutes: HttpRoutes[IO] = CORS.policy.withAllowOriginAll(routes)
}
