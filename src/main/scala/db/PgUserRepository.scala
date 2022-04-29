package io.blindnet.backend
package db

import models.{User, UserRepository}

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgUserRepository(xa: Transactor[IO]) extends UserRepository[IO] {
  override def countByIdsOutsideGroup(appId: String, groupId: String, usersId: List[String]): IO[Long] =
    sql"select count(*) from users where app=$appId::uuid and id in $usersId and group_id != $groupId"
      .query[Long].unique.transact(xa)

  override def findById(appId: String, id: String): IO[Option[User]] =
    sql"select app, id, group_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt from users where app=$appId::uuid and id=$id"
      .query[User].option.transact(xa)

  override def findAllByGroup(appId: String, groupId: String): IO[List[User]] =
    sql"select app, id, group_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt from users where app=$appId::uuid and group_id=$groupId"
      .query[User].to[List].transact(xa)

  override def findAllByIds(appId: String, ids: List[String]): IO[List[User]] =
    NonEmptyList.fromList(ids) match
      case Some(nel) => (fr"select app, id, group_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt from users where app=$appId::uuid and"
        ++ Fragments.in(fr"id", nel))
        .query[User].to[List].transact(xa)
      case None => IO.pure(Nil)

  override def insert(user: User): IO[Unit] =
    sql"""insert into users (app, id, group_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt)
          values (${user.appId}::uuid, ${user.id}, ${user.groupId}, ${user.publicEncKey}, ${user.publicSignKey}, ${user.signedPublicEncKey}, ${user.encPrivateEncKey}, ${user.encPrivateSignKey}, ${user.keyDerivationSalt})"""
      .update.run.transact(xa).void

  override def updatePrivateKeys(appId: String, id: String, encPrivateEncKey: String, encPrivateSignKey: String): IO[Unit] =
    sql"""update users set enc_priv_enc_key=${encPrivateEncKey}, enc_priv_sign_key=${encPrivateSignKey}
          where app=$appId::uuid and users.id=${id}"""
      .update.run.transact(xa).flatMap(DbUtil.ensureUpdatedOne)

  override def updatePrivateKeysAndSalt(appId: String, id: String, encPrivateEncKey: String, encPrivateSignKey: String, keyDerivationSalt: String): IO[Unit] =
    sql"""update users set enc_priv_enc_key=${encPrivateEncKey}, enc_priv_sign_key=${encPrivateSignKey}, key_deriv_salt=${keyDerivationSalt}
          where app=$appId::uuid and users.id=${id}"""
      .update.run.transact(xa).flatMap(DbUtil.ensureUpdatedOne)

  override def delete(appId: String, id: String): IO[Unit] =
    sql"delete from users where app=$appId::uuid and id=$id"
      .update.run.transact(xa).flatMap(DbUtil.ensureUpdatedOne)

  override def deleteAllByGroup(appId: String, groupId: String): IO[Unit] =
    sql"delete from users where app=$appId::uuid and group_id=$groupId"
      .update.run.transact(xa).flatMap(DbUtil.ensureUpdatedAtLeastOne)
}
