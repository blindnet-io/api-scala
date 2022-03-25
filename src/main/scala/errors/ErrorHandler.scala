package io.blindnet.backend
package errors

import auth.AuthException

import cats.effect.*
import org.http4s.*
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*

object ErrorHandler {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val handler: Request[IO] => PartialFunction[Throwable, IO[Response[IO]]] = req => {
    case e: AuthException => for {
      _ <- logger.debug(e)("Authentication exception")
    } yield Response(Status.Forbidden).withEntity(e.getMessage)

    case e: Exception => for {
      _ <- logger.error(e)("Unhandled exception")
    } yield Response(Status.InternalServerError)
  }
}
