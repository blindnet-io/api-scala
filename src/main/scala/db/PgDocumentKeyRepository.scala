package io.blindnet.backend
package db

import models.{DocumentKey, DocumentKeyRepository}

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgDocumentKeyRepository(xa: Transactor[IO]) extends DocumentKeyRepository[IO] {
  override def findAllByDocument(appId: String, docId: String): IO[List[DocumentKey]] =
    sql"select app_id, document_id, user_id, enc_sym_key from document_keys where app_id=$appId::uuid and document_id=$docId::uuid"
      .query[DocumentKey].to[List].transact(xa)

  override def findAllByUser(appId: String, userId: String): IO[List[DocumentKey]] =
    sql"select app_id, document_id, user_id, enc_sym_key from document_keys where app_id=$appId::uuid and user_id=$userId"
      .query[DocumentKey].to[List].transact(xa)

  override def findAllByDocumentsAndUser(appId: String, docIds: List[String], userId: String): IO[List[DocumentKey]] =
    sql"select app_id, document_id, user_id, enc_sym_key from document_keys where app_id=$appId::uuid and user_id=$userId and document_id in $docIds::uuid[]"
      .query[DocumentKey].to[List].transact(xa)

  override def findByDocumentAndUser(appId: String, docId: String, userId: String): IO[Option[DocumentKey]] =
    sql"select app_id, document_id, user_id, enc_sym_key from document_keys where app_id=$appId::uuid and document_id=$docId::uuid and user_id=$userId"
      .query[DocumentKey].option.transact(xa)

  override def insert(key: DocumentKey): IO[Unit] =
    sql"""insert into document_keys (document_id, user_id, enc_sym_key, app_id)
          values (${key.documentId}::uuid, ${key.userId}, ${key.encSymmetricKey}, ${key.appId}::uuid)"""
      .update.run.transact(xa).map(_ => ())

  override def updateOne(key: DocumentKey): IO[Unit] =
    sql"""update document_keys set enc_sym_key=${key.encSymmetricKey}
          where document_id=${key.documentId}::uuid and user_id=${key.userId} and app_id=${key.appId}::uuid"""
      .update.run.transact(xa).map(_ => ())

  override def deleteByUser(appId: String, userId: String): IO[Unit] =
    sql"delete from document_keys where app=$appId::uuid and user_id=$userId"
      .update.run.transact(xa).map(_ => ())
}
