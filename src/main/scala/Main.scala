package io.blindnet.backend

import db.{DbConfig, Migrator}

import cats.effect.*
import doobie.*
import doobie.hikari.*
import org.http4s.server.Server
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*

object Main extends IOApp {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

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

    for {
      _ <- logger.info("Env: " + Env.get.name)
      _ <- if Env.get.migrate then Migrator.migrateDatabase(dbConfig) else IO.unit
      _ <- server.use(_ => IO.never)
    } yield ExitCode.Success
}
