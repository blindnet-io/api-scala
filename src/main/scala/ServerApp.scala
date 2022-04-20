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
  val documentRepo: PgDocumentRepository = PgDocumentRepository(xa)
  val documentKeyRepo: PgDocumentKeyRepository = PgDocumentKeyRepository(xa)
  
  def app: HttpApp[IO] =
    Router(
      "/api/v1" -> ServicesRouter(appRepo, userRepo, documentRepo, documentKeyRepo).corsRoutes,
    ).orNotFound
  
  def server: Resource[IO, Server] =
    for {
      httpServer <- BlazeServerBuilder[IO]
        .bindHttp(8087, "127.0.0.1")
        .withHttpApp(app)
        .withServiceErrorHandler(ErrorHandler.handler)
        .resource
    } yield httpServer
}
