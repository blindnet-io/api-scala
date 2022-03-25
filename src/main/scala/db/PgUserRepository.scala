package io.blindnet.backend
package db

import models.{User, UserRepository}

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgUserRepository(xa: Transactor[IO]) extends UserRepository[IO] {
  override def countByIdsOutsideGroup(groupId: String, usersId: List[String]): IO[Long] =
    sql"select count(*) from users where id in $usersId and group_id != $groupId"
      .query[Long].unique.transact(xa)

  override def findAllByGroup(groupId: String): IO[List[User]] =
    sql"select app, id, group_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt from users where group_id=$groupId"
      .query[User].to[List].transact(xa)

  override def findById(id: String): IO[Option[User]] =
    sql"select app, id, group_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt from users where id=$id"
      .query[User].option.transact(xa)

  override def insert(user: User): IO[Unit] =
    sql"""insert into users (app, id, group_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt)
          values (${user.appId}::uuid, ${user.id}, ${user.groupId}, ${user.publicEncKey}, ${user.publicSignKey}, ${user.signedPublicEncKey}, ${user.encPrivateEncKey}, ${user.encPrivateSignKey}, ${user.keyDerivationSalt})"""
      .update.run.transact(xa).map(_ => ())

  override def updatePrivateKeys(id: String, encPrivateEncKey: String, encPrivateSignKey: String): IO[Unit] =
    sql"""update users set enc_priv_enc_key=${encPrivateEncKey}, enc_priv_sign_key=${encPrivateSignKey}
          where users.id=${id}"""
      .update.run.transact(xa).map(_ => ())

  override def updatePrivateKeysAndSalt(id: String, encPrivateEncKey: String, encPrivateSignKey: String, keyDerivationSalt: String): IO[Unit] =
    sql"""update users set enc_priv_enc_key=${encPrivateEncKey}, enc_priv_sign_key=${encPrivateSignKey}, key_deriv_salt=${keyDerivationSalt}
          where users.id=${id}"""
      .update.run.transact(xa).map(_ => ())

  override def delete(id: String): IO[Unit] =
    sql"delete from users where id=$id"
      .update.run.transact(xa).map(_ => ())
}
