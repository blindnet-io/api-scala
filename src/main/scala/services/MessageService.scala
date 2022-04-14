package io.blindnet.backend
package services

import auth.*
import errors.*
import models.*

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.syntax.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.AuthMiddleware

import java.time.Instant
import java.util.{Random, UUID}
import scala.util.Try

class MessageService(userRepo: UserRepository[IO], messageRepo: MessageRepository[IO]) {
  def authedRoutes: AuthedRoutes[AuthJwt, IO] = AuthedRoutes.of[AuthJwt, IO] {
    // FR-M01 Send Message
    case req @ POST -> Root / "messages" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[SendMessagePayload]
        _ <- userRepo.findById(uJwt.appId, payload.recipientID).orNotFound
        timeCreated <- Try(Instant.parse(payload.timestamp)).orBadRequest
        msg = models.Message(Random().nextLong(), uJwt.appId, uJwt.userId, payload.recipientID, payload.message, timeCreated)
        _ <- messageRepo.insert(msg)
        res <- Ok(msg.id.toString)
      } yield res
  }
}

case class SendMessagePayload(
  recipientID: String,
  message: String,
  timestamp: String,
)
