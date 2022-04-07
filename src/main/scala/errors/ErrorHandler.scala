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

    case e: MessageFailure => for {
      _ <- logger.debug(e)("Message handling exception")
    } yield Response(Status.BadRequest)

    case e: AuthException => for {
      _ <- logger.debug(e)("Authentication exception")
    } yield Response(Status.Forbidden).withEntity(e.getMessage)

    case e: NotFoundException => for {
      _ <- logger.debug(e)("NotFound exception")
    } yield Response(Status.NotFound)

    case e: Exception => for {
      _ <- logger.error(e)("Unhandled exception")
    } yield Response(Status.InternalServerError)
  }
}
