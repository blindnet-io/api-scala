package io.blindnet.backend
package db

import models.{Document, DocumentRepository}

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgDocumentRepository(xa: Transactor[IO]) extends DocumentRepository[IO] {
  override def findAllByIds(appId: String, ids: List[String]): IO[List[Document]] =
    NonEmptyList.fromList(ids) match
      case Some(nel) => fr"select app, id from documents where app=$appId::uuid and id::text in $ids"
        ++ Fragments.in(fr"id", nel) ++ sql"::uuid[]")
        .query[Document].to[List].transact(xa)
      case None => IO.pure(Nil)

  override def findById(appId: String, id: String): IO[Option[Document]] =
    sql"select app, id from documents where app=$appId::uuid and id=$id::uuid"
      .query[Document].option.transact(xa)

  override def insert(doc: Document): IO[Unit] =
    sql"""insert into documents (id, app)
          values (${doc.id}::uuid, ${doc.appId}::uuid)"""
      .update.run.transact(xa).map(_ => ())

  override def delete(appId: String, id: String): IO[Unit] =
    sql"delete from documents where app=$appId::uuid and id=$id"
      .update.run.transact(xa).flatMap(DbUtil.ensureUpdatedOne)
}
