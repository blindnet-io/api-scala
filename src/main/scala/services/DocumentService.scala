package io.blindnet.backend
package services

import auth.*
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

import java.util.UUID

class DocumentService(documentRepo: DocumentRepository[IO], documentKeyRepo: DocumentKeyRepository[IO]) {
  private def authedRoutes = AuthedRoutes.of[AuthJwt, IO] {
    case req @ POST -> Root / "documents" as jwt =>
      for {
        uJwt: TempUserAuthJwt <- jwt.asTempUser
        payload <- req.req.as[CreateDocumentPayload]
        _ <- payload.traverse(item => checkTempTokenContainsUser(uJwt, item.userID))
        doc = Document(uJwt.appId, UUID.randomUUID().toString)
        _ <- documentRepo.insert(doc)
        _ <- payload.traverse(item => documentKeyRepo.insert(DocumentKey(uJwt.appId, doc.id, item.userID, item.encryptedSymmetricKey)))
        res <- Ok(doc.id)
      } yield res
  }

  private def checkTempTokenContainsUser(jwt: TempUserAuthJwt, userId: String): IO[Unit] =
    if jwt.userIds.contains(userId) then IO.unit else IO.raiseError(Exception("Token does not contain user ID"))

  private def authMiddleware = AuthMiddleware(AuthJwt.authenticate, Kleisli(req => OptionT.liftF(Forbidden(req.context))))
  def routes: HttpRoutes[IO] = authMiddleware(authedRoutes)
}

type CreateDocumentPayload = Seq[CreateDocumentItem]
case class CreateDocumentItem(
  userID: String,
  encryptedSymmetricKey: String
)
