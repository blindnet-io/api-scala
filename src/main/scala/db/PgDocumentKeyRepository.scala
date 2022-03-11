package io.blindnet.backend
package db

import models.{DocumentKey, DocumentKeyRepository}

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*

class PgDocumentKeyRepository(xa: Transactor[IO]) extends DocumentKeyRepository[IO] {
  override def findAllByDocument(appId: String, docId: String): IO[List[DocumentKey]] =
    sql"select app_id, document_id, user_id, enc_sym_key from document_keys where app_id=$appId and document_id=$docId::uuid"
      .query[DocumentKey].to[List].transact(xa)

  override def insert(key: DocumentKey): IO[Unit] =
    sql"""insert into document_keys (document_id, user_id, enc_sym_key, app_id)
          values (${key.documentId}::uuid, ${key.userId}, ${key.encSymmetricKey}, ${key.appId})"""
      .update.run.transact(xa).map(_ => ())
}
