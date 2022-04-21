package io.blindnet.backend
package db

import models.{App, AppRepository}

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgAppRepository(xa: Transactor[IO]) extends AppRepository[IO] {
  override def findById(id: String): IO[Option[App]] =
    sql"select id, public_key, name from apps where id=$id::uuid"
      .query[App].option.transact(xa)

  override def insert(app: App): IO[Unit] =
    sql"""insert into apps (id, public_key, name)
          values (${app.id}::uuid, ${app.publicKey}, ${app.name})"""
      .update.run.transact(xa).void
}
