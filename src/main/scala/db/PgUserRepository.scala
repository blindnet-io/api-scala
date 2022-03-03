package io.blindnet.backend
package db

import models.{User, UserRepository}

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*

class PgUserRepository(xa: Transactor[IO]) extends UserRepository[IO] {
  override def findById(id: String): IO[Option[User]] =
    sql"select * from users where id=$id"
      .query[User].option.transact(xa)

  override def insert(user: User): IO[Unit] =
    sql"""insert into users (id, public_enc_key, public_sign_key, enc_private_enc_key, enc_private_sign_key, key_derivation_salt)
          values (${user.id}, ${user.publicEncKey}, ${user.publicSignKey}, ${user.encPrivateEncKey}, ${user.encPrivateSignKey}, ${user.keyDerivationSalt})"""
      .update.run.transact(xa).map(_ => ())
}
