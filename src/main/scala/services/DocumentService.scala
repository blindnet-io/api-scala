package io.blindnet.backend
package services

import auth.*
import errors.*
import models.*

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.*
import cats.effect.std.UUIDGen
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

class DocumentService(userRepo: UserRepository[IO], documentRepo: DocumentRepository[IO], documentKeyRepo: DocumentKeyRepository[IO], storageObjectRepo: StorageObjectRepository[IO]) {
  implicit val uuidGen: UUIDGen[IO] = UUIDGen.fromSync

  def authedRoutes: AuthedRoutes[AuthJwt, IO] = AuthedRoutes.of[AuthJwt, IO] {
    // FR-BE06 Create Document
    case req @ POST -> Root / "documents" as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        payload <- req.req.as[CreateDocumentPayload]
        _ <- if payload.nonEmpty then IO.unit else IO.raiseError(BadRequestException("Empty payload"))
        _ <- auJwt.containsUserIds(payload.map(item => item.userID), userRepo)
        _ <- userRepo.findAllByIds(auJwt.appId, payload.map(_.userID)).ensureSize(payload.size)
        docId <- UUIDGen.randomString
        doc = Document(auJwt.appId, docId)
        _ <- documentRepo.insert(doc)
        _ <- documentKeyRepo.insertMany(payload.map(item => DocumentKey(auJwt.appId, doc.id, item.userID, item.encryptedSymmetricKey)))
        res <- Ok(doc.id)
      } yield res

    // Create Document From StorageObject
    case req @ POST -> Root / "documents" / objId as jwt =>
      for {
        auJwt: AnyUserJwt <- jwt.asAnyUser
        payload <- req.req.as[CreateDocumentPayload]
        _ <- if payload.nonEmpty then IO.unit else IO.raiseError(BadRequestException("Empty payload"))
        _ <- auJwt.containsUserIds(payload.map(item => item.userID), userRepo)
        obj <- storageObjectRepo.findById(auJwt.appId, objId).orNotFound
        _ <- if auJwt match
          case uJwt: UserJwt => obj.userId.contains(uJwt.userId)
          case tuJwt: TempUserJwt => obj.tokenId.contains(tuJwt.tokenId)
        then IO.unit else IO.raiseError(AuthException())
        _ <- userRepo.findAllByIds(auJwt.appId, payload.map(_.userID)).ensureSize(payload.size)
        _ <- documentRepo.findById(auJwt.appId, objId).thenBadRequest("Document already exists")
        doc = Document(auJwt.appId, objId)
        _ <- documentRepo.insert(doc)
        _ <- documentKeyRepo.insertMany(payload.map(item => DocumentKey(auJwt.appId, doc.id, item.userID, item.encryptedSymmetricKey)))
        res <- Ok(doc.id)
      } yield res

    // FR-BE10 Add User Document Keys
    case req @ PUT -> Root / "documents" / "keys" / "user" / userId as jwt =>
      for {
        uJwt: UserJwt <- jwt.asUser
        payload <- req.req.as[AddUserKeysPayload]
        _ <- userRepo.findById(uJwt.appId, userId).orNotFound
        _ <- documentRepo.findAllByIds(uJwt.appId, payload.map(_.documentID)).ensureSize(payload.size)
        _ <- documentKeyRepo.findAllByDocumentsAndUser(uJwt.appId, payload.map(_.documentID), userId)
          .ensureSize(0, BadRequestException("key already exists"))
        _ <- documentKeyRepo.insertMany(payload.map(item => DocumentKey(uJwt.appId, item.documentID, userId, item.encryptedSymmetricKey)))
        ret <- Ok(true)
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
        docIds = payload.data_ids.distinct
        docs <- documentRepo.findAllByIds(uJwt.appId, docIds).ensureSize(docIds.size)
        keys <- documentKeyRepo.findAllByDocumentsAndUser(uJwt.appId, docIds, uJwt.userId).ensureSize(docIds.size, AuthException())
        ret <- Ok(keys.map(key => GetAllDocsAndKeysResponseItem(key.documentId, key.encSymmetricKey)))
      } yield ret

    // FR-BE11 Delete Document
    case req @ DELETE -> Root / "documents" / docId as jwt =>
      for {
        cJwt: ClientJwt <- jwt.asClient
        _ <- documentRepo.delete(cJwt.appId, docId)
        ret <- Ok()
      } yield ret

    // FR-BE12 Delete Documents User
    case req @ DELETE -> Root / "documents" / "user" / userId as jwt =>
      for {
        cJwt: ClientJwt <- jwt.asClient
        _ <- documentKeyRepo.deleteByUser(cJwt.appId, userId)
        ret <- Ok()
      } yield ret

    // FR-BE18 Delete Document Key
    // TODO discuss this path: we have a mix of "keys" and "users" in this service but they are basically the same
    case req @ DELETE -> Root / "documents" / docId / "keys" / userId as jwt =>
      for {
        cJwt: ClientJwt <- jwt.asClient
        _ <- documentKeyRepo.deleteByDocumentAndUser(cJwt.appId, docId, userId)
        ret <- Ok()
      } yield ret
  }
}

type CreateDocumentPayload = List[CreateDocumentItem]
case class CreateDocumentItem(
  userID: String,
  encryptedSymmetricKey: String
)

type AddUserKeysPayload = List[AddUserKeysItem]
case class AddUserKeysItem(
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
