package io.blindnet.backend

import db.{DbConfig, Migrator}

import cats.effect.*
import doobie.*
import doobie.hikari.*
import org.http4s.server.Server

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    val dbConfig = DbConfig(sys.env("BN_DB_URI"), sys.env("BN_DB_USER"), sys.env("BN_DB_PASSWORD"))

    val server: Resource[IO, Server] = for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        dbConfig.uri,
        dbConfig.username,
        dbConfig.password,
        ce
      )
      server <- ServerApp(xa).server
    } yield server

    Migrator.migrateDatabase(dbConfig)
      .flatMap(_ => server.use(_ => IO.never))
      .as(ExitCode.Success)
}
