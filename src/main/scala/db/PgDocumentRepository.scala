package io.blindnet.backend
package db

import models.{Document, DocumentRepository}

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*

class PgDocumentRepository(xa: Transactor[IO]) extends DocumentRepository[IO] {
  override def findById(id: String): IO[Option[Document]] =
    sql"select id, app from documents where id=$id"
      .query[Document].option.transact(xa)

  override def insert(doc: Document): IO[Unit] =
    sql"""insert into documents (id, app)
          values (${doc.id}, ${doc.appId})"""
      .update.run.transact(xa).map(_ => ())
}
