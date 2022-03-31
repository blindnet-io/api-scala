package io.blindnet.backend

import cats.effect.*
import io.blindnet.backend.db.Migrator

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- Migrator.migrateDatabase()
      _ <- Server.server.use(_ => IO.never)
    } yield ExitCode.Success
}
