package io.blindnet.backend

import db.{DbConfig, Migrator}

import cats.effect.*

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    val dbConfig = DbConfig(sys.env("BN_DB_URI"), sys.env("BN_DB_USER"), sys.env("BN_DB_PASSWORD"))
    
    for {
      _ <- Migrator.migrateDatabase(dbConfig)
      _ <- Server.create(dbConfig).use(_ => IO.never)
    } yield ExitCode.Success
}
