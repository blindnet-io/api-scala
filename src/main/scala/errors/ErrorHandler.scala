package io.blindnet.backend
package errors

import auth.AuthException

import cats.data.OptionT
import cats.effect.*
import org.http4s.*
import org.http4s.server.middleware.ErrorHandling
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.*

object ErrorHandler {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val handler: PartialFunction[Throwable, IO[Response[IO]]] = {
    case e: BadRequestException => for {
      _ <- logger.debug(e)("Bad request exception")
    } yield Response(Status.BadRequest).condEntity(Env.get.sendErrorMessages, e.getMessage)

    case e: MessageFailure => for {
      _ <- logger.debug(e)("Message handling exception")
    } yield Response(Status.BadRequest).condEntity(Env.get.sendErrorMessages, e.getMessage)

    case e: AuthException => for {
      _ <- logger.debug(e)("Authentication exception")
    } yield Response(Status.Forbidden).condEntity(Env.get.sendErrorMessages, e.getMessage)

    case e: NotFoundException => for {
      _ <- logger.debug(e)("NotFound exception")
    } yield Response(Status.NotFound).condEntity(Env.get.sendErrorMessages, e.getMessage)

    case e: Exception => for {
      _ <- logger.error(e)("Unhandled exception")
    } yield Response(Status.InternalServerError).condEntity(Env.get.sendInternalErrorMessages, e.getMessage)
  }

  def apply(httpRoutes: HttpRoutes[IO]): HttpRoutes[IO] =
    ErrorHandling.Custom.recoverWith(httpRoutes)(handler.andThen(OptionT.liftF))
}

extension(m: Response[IO]) {
  def condEntity[T](cond: Boolean, entity: T)(implicit enc: EntityEncoder[IO, T]): Response[IO] =
    if cond && entity != null then m.withEntity(entity) else m
}
