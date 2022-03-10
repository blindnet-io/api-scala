package io.blindnet.backend

import db.PgUserRepository
import services.ServicesRouter

import cats.effect.*
import cats.implicits.*
import doobie.Transactor
import org.http4s.blaze.server.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.*

object Main extends IOApp {
  private def server =
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", sys.env("BN_DB_URI"), sys.env("BN_DB_USER"), sys.env("BN_DB_PASSWORD")
    )
    val userRepo = PgUserRepository(xa)

    for {
      httpServer <- BlazeServerBuilder[IO]
        .bindHttp(8087, "127.0.0.1")
        .withHttpApp(Router(
          "/api/v1" -> ServicesRouter(userRepo).corsRoutes,
        ).orNotFound)
        .resource
    } yield httpServer

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
}
