package io.blindnet.backend
package services

import auth.*
import endpoints.*
import errors.ErrorHandler
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
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}

class ServicesRouter(
    appRepo: AppRepository[IO],
    userRepo: UserRepository[IO],
    userKeysRepo: UserKeysRepository[IO],
    deviceRepo: UserDeviceRepository[IO],
    oneTimeKeyRepo: OneTimeKeyRepository[IO],
    documentRepo: DocumentRepository[IO],
    documentKeyRepo: DocumentKeyRepository[IO],
    messageRepo: MessageRepository[IO],
    storageObjectRepo: StorageObjectRepository[IO],
    storageBlockRepo: StorageBlockRepository[IO]) {
  private val authenticator = JwtAuthenticator(appRepo, userRepo)

  private val userService = UserService(userRepo, userKeysRepo)
  private val signalUserService = SignalUserService(userRepo, deviceRepo, oneTimeKeyRepo)
  private val documentService = DocumentService(userRepo, documentRepo, documentKeyRepo, storageObjectRepo)
  private val messageService = MessageService(userRepo, deviceRepo, messageRepo)
  private val storageService = StorageService(storageObjectRepo, storageBlockRepo, documentKeyRepo)

  private def unsafeRoutes =
    userService.authedRoutes
      <+> signalUserService.authedRoutes
      <+> storageService.authedRoutes

  private val documentEndpoints = DocumentEndpoints(authenticator, documentService)
  private val messageEndpoints = MessageEndpoints(authenticator, messageService)

  private val allEndpoints = documentEndpoints.list ++ messageEndpoints.list
  private val swaggerEndpoints = SwaggerEndpoints(allEndpoints).endpoints

  private val http4sOptions: Http4sServerOptions[IO] = Http4sServerOptions
    .customiseInterceptors[IO]
    .exceptionHandler(None)
    .options

  def routes: HttpRoutes[IO] =
    CORS.policy.withAllowOriginAll(
      ErrorHandler(
        Http4sServerInterpreter[IO](http4sOptions).toRoutes(allEndpoints ++ swaggerEndpoints) <+>
        authenticator.authMiddleware(
          unsafeRoutes
        )
      )
    )
}
