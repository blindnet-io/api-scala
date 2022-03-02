package io.blindnet.backend

import cats.effect.*
import cats.implicits.*
import org.http4s.blaze.server.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.*

object Main extends IOApp {
  private def server =
    for {
      httpServer <- BlazeServerBuilder[IO]
        .bindHttp(8087, "127.0.0.1")
        .resource
    } yield httpServer

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
}
