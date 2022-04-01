package io.blindnet.backend

import db.*
import services.ServicesRouter

import cats.effect.*
import cats.implicits.*
import doobie.Transactor
import io.blindnet.backend.errors.ErrorHandler
import org.http4s.{Request, Response}
import org.http4s.blaze.server.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.*

object Server {
  def create(dbConfig: DbConfig): Resource[IO, Server] =
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", dbConfig.uri, dbConfig.username, dbConfig.password 
    )
    val appRepo = PgAppRepository(xa)
    val userRepo = PgUserRepository(xa)
    val documentRepo = PgDocumentRepository(xa)
    val documentKeyRepo = PgDocumentKeyRepository(xa)

    for {
      httpServer <- BlazeServerBuilder[IO]
        .bindHttp(8087, "127.0.0.1")
        .withHttpApp(Router(
          "/api/v1" -> ServicesRouter(appRepo, userRepo, documentRepo, documentKeyRepo).corsRoutes,
        ).orNotFound)
        .withServiceErrorHandler(ErrorHandler.handler)
        .resource
    } yield httpServer
}
