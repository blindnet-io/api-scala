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
    // FR-BE06 Save Document Keys
    // TODO Handle groups (and update FRD that does not specify user IDs are allowed)
    case req @ POST -> Root / "documents" as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        payload <- req.req.as[CreateDocumentPayload]
        _ <- payload.traverse(item => auJwt.containsUserId(item.userID))
        doc = Document(auJwt.appId, UUID.randomUUID().toString)
        _ <- documentRepo.insert(doc)
        _ <- payload.traverse(item => documentKeyRepo.insert(DocumentKey(auJwt.appId, doc.id, item.userID, item.encryptedSymmetricKey)))
        res <- Ok(doc.id)
      } yield res

    // FR-BE10 Update All User Keys
    case req @ PUT -> Root / "documents" / "keys" / "user" / userId as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[UpdateUserKeysPayload]
        _ <- payload.traverse(item => documentKeyRepo.updateOne(DocumentKey(uJwt.appId, item.documentID, userId, item.encryptedSymmetricKey)))
        ret <- Ok()
      } yield ret

    // FR-BE07 Get Document Key
    case req @ GET -> Root / "documents" / "keys" / docId as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        ret <- documentKeyRepo.findByDocumentAndUser(uJwt.appId, docId, uJwt.userId).flatMap {
          case Some(key) => Ok(key.encSymmetricKey)
          case None => NotFound()
        }
      } yield ret

    // FR-BE08 Get All Documents And Keys
    case req @ GET -> Root / "documents" / "keys" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        keys <- documentKeyRepo.findAllByUser(uJwt.appId, uJwt.userId)
        ret <- Ok(keys.map(key => GetAllDocsAndKeysResponseItem(key.documentId, key.encSymmetricKey)))
      } yield ret

    // FR-BE17 Get Documents And Keys
    case req @ POST -> Root / "documents" / "keys" as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[GetDocsAndKeysPayload]
        docs <- documentRepo.findAllByIds(payload.data_ids)
        // TODO check docs length => 404
        keys <- documentKeyRepo.findAllByDocumentsAndUser(uJwt.appId, payload.data_ids, uJwt.userId)
        // TODO check keys length => 403
        ret <- Ok(keys.map(key => GetAllDocsAndKeysResponseItem(key.documentId, key.encSymmetricKey)))
      } yield ret

    // FR-BE11 Delete Document
    case req @ DELETE -> Root / "documents" / docId as jwt =>
      for {
        cJwt: ClientJwt <- jwt.asClient
        _ <- documentRepo.delete(docId)
        ret <- Ok()
      } yield ret

    // FR-BE12 Delete Documents User
    case req @ DELETE -> Root / "documents" / "user" / userId as jwt =>
      for {
        cJwt: ClientJwt <- jwt.asClient
        _ <- documentKeyRepo.deleteByUser(userId)
        ret <- Ok()
      } yield ret
  }

  private def authMiddleware = AuthMiddleware(AuthJwt.authenticate, Kleisli(req => OptionT.liftF(Forbidden(req.context))))
  def routes: HttpRoutes[IO] = authMiddleware(authedRoutes)
}

type CreateDocumentPayload = Seq[CreateDocumentItem]
case class CreateDocumentItem(
  userID: String,
  encryptedSymmetricKey: String
)

type UpdateUserKeysPayload = Seq[UpdateUserKeysItem]
case class UpdateUserKeysItem(
  documentID: String,
  encryptedSymmetricKey: String
)

case class GetAllDocsAndKeysResponseItem(
  documentID: String,
  encryptedSymmetricKey: String
)

case class GetDocsAndKeysPayload(
  data_ids: List[String]
)
