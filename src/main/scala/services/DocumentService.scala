package io.blindnet.backend
package services

import auth.*
import errors.*
import models.*
import objects.*

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

  def createDocument(jwt: AuthJwt)(payload: CreateDocumentPayload): IO[String] =
    for {
      auJwt: AnyUserJwt <- jwt.asAnyUser
      _ <- if payload.nonEmpty then IO.unit else IO.raiseError(BadRequestException("Empty payload"))
      _ <- auJwt.containsUserIds(payload.map(item => item.userID), userRepo)
      _ <- userRepo.findAllByIds(auJwt.appId, payload.map(_.userID)).ensureSize(payload.size)
      docId <- UUIDGen.randomString
      doc = Document(auJwt.appId, docId)
      _ <- documentRepo.insert(doc)
      _ <- documentKeyRepo.insertManyIgnoreConflicts(payload.map(item => DocumentKey(auJwt.appId, doc.id, item.userID, item.encryptedSymmetricKey)))
    } yield doc.id

  def addUserDocumentKeys(jwt: AuthJwt)(userId: String, payload: AddUserKeysPayload): IO[Boolean] =
    for {
      uJwt: UserJwt <- jwt.asUser
      _ <- userRepo.findById(uJwt.appId, userId).orNotFound
      _ <- documentRepo.findAllByIds(uJwt.appId, payload.map(_.documentID)).ensureSize(payload.size)
      _ <- documentKeyRepo.insertManyIgnoreConflicts(payload.map(item => DocumentKey(uJwt.appId, item.documentID, userId, item.encryptedSymmetricKey)))
    } yield true

  def getDocumentKey(jwt: AuthJwt)(docId: String): IO[String] =
    for {
      uJwt: UserJwt <- jwt.asUser
      key <- documentKeyRepo.findByDocumentAndUser(uJwt.appId, docId, uJwt.userId).orNotFound
    } yield key.encSymmetricKey

  def getAllDocumentsAndKeys(jwt: AuthJwt)(x: Unit): IO[List[GetAllDocsAndKeysResponseItem]] =
    for {
      uJwt: UserJwt <- jwt.asUser
      keys <- documentKeyRepo.findAllByUser(uJwt.appId, uJwt.userId)
    } yield keys.map(key => GetAllDocsAndKeysResponseItem(key.documentId, key.encSymmetricKey))

  def getDocumentsAndKeys(jwt: AuthJwt)(payload: GetDocsAndKeysPayload): IO[List[GetAllDocsAndKeysResponseItem]] =
    for {
      uJwt: UserJwt <- jwt.asUser
      docIds = payload.data_ids.distinct
      docs <- documentRepo.findAllByIds(uJwt.appId, docIds).ensureSize(docIds.size)
      keys <- documentKeyRepo.findAllByDocumentsAndUser(uJwt.appId, docIds, uJwt.userId).ensureSize(docIds.size, ForbiddenException())
    } yield keys.map(key => GetAllDocsAndKeysResponseItem(key.documentId, key.encSymmetricKey))

  def createDocumentFromStorage(jwt: AuthJwt)(objId: String, payload: CreateDocumentPayload): IO[String] =
    for {
      auJwt: AnyUserJwt <- jwt.asAnyUser
      _ <- if payload.nonEmpty then IO.unit else IO.raiseError(BadRequestException("Empty payload"))
      _ <- auJwt.containsUserIds(payload.map(item => item.userID), userRepo)
      obj <- storageObjectRepo.findById(auJwt.appId, objId).orNotFound
      _ <- if auJwt match
        case uJwt: UserJwt => obj.userId.contains(uJwt.userId)
        case tuJwt: TempUserJwt => obj.tokenId.contains(tuJwt.tokenId)
      then IO.unit else IO.raiseError(ForbiddenException())
      _ <- userRepo.findAllByIds(auJwt.appId, payload.map(_.userID)).ensureSize(payload.size)
      _ <- documentRepo.findById(auJwt.appId, objId).thenBadRequest("Document already exists")
      doc = Document(auJwt.appId, objId)
      _ <- documentRepo.insert(doc)
      _ <- documentKeyRepo.insertManyIgnoreConflicts(payload.map(item => DocumentKey(auJwt.appId, doc.id, item.userID, item.encryptedSymmetricKey)))
    } yield doc.id

  def deleteDocument(jwt: AuthJwt)(docId: String): IO[Boolean] =
    for {
      cJwt: ClientJwt <- jwt.asClient
      _ <- documentRepo.delete(cJwt.appId, docId)
    } yield true

  def deleteDocumentsUser(jwt: AuthJwt)(userId: String): IO[Boolean] =
    for {
      cJwt: ClientJwt <- jwt.asClient
      _ <- documentKeyRepo.deleteByUser(cJwt.appId, userId)
    } yield true
    
  def deleteDocumentKey(jwt: AuthJwt)(docId: String, userId: String): IO[Boolean] =
    for {
      cJwt: ClientJwt <- jwt.asClient
      _ <- documentKeyRepo.deleteByDocumentAndUser(cJwt.appId, docId, userId)
    } yield true
}
