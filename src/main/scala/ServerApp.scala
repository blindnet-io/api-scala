package io.blindnet.backend

import db.*
import errors.ErrorHandler
import services.ServicesRouter

import cats.effect.*
import cats.implicits.*
import doobie.Transactor
import org.http4s.blaze.server.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.*
import org.http4s.{HttpApp, Request, Response}

class ServerApp(xa: Transactor[IO]) {
  val appRepo: PgAppRepository = PgAppRepository(xa)
  val userRepo: PgUserRepository = PgUserRepository(xa)
  val userKeysRepo: PgUserKeysRepository = PgUserKeysRepository(xa)
  val userDeviceRepo: PgUserDeviceRepository = PgUserDeviceRepository(xa)
  val oneTimeKeyRepo: PgOneTimeKeyRepository = PgOneTimeKeyRepository(xa)
  val documentRepo: PgDocumentRepository = PgDocumentRepository(xa)
  val documentKeyRepo: PgDocumentKeyRepository = PgDocumentKeyRepository(xa)
  val messageRepo: PgMessageRepository = PgMessageRepository(xa)
  
  def app: HttpApp[IO] =
    Router(
      "/api/v1" -> ServicesRouter(
        appRepo, userRepo, userKeysRepo, userDeviceRepo, oneTimeKeyRepo, documentRepo, documentKeyRepo, messageRepo
      ).corsRoutes,
    ).orNotFound
  
  def server: Resource[IO, Server] =
    for {
      httpServer <- BlazeServerBuilder[IO]
        .bindHttp(Env.get.port, Env.get.host)
        .withHttpApp(app)
        .withServiceErrorHandler(ErrorHandler.handler)
        .resource
    } yield httpServer
}
