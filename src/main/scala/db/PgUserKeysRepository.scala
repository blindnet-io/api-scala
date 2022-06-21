package io.blindnet.backend
package db

import models.{UserKeys, UserKeysRepository}

import cats.data.NonEmptyList
import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgUserKeysRepository(xa: Transactor[IO]) extends UserKeysRepository[IO] {
  override def findById(appId: String, userId: String): IO[Option[UserKeys]] =
    sql"select app_id, user_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt from user_keys where app_id=$appId::uuid and user_id=$userId"
      .query[UserKeys].option.transact(xa)

  override def findAllByGroup(appId: String, groupId: String): IO[List[UserKeys]] =
    sql"""select app_id, user_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt from user_keys
          inner join users on user_keys.user_id = users.id
          where app_id=$appId::uuid and group_id=$groupId"""
      .query[UserKeys].to[List].transact(xa)

  override def findAllByIds(appId: String, ids: List[String]): IO[List[UserKeys]] =
    NonEmptyList.fromList(ids) match
      case Some(nel) => (fr"select app_id, user_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt from user_keys where app_id=$appId::uuid and"
        ++ Fragments.in(fr"user_id", nel))
        .query[UserKeys].to[List].transact(xa)
      case None => IO.pure(Nil)

  override def insert(keys: UserKeys): IO[Unit] =
    sql"""insert into user_keys (app_id, user_id, pub_enc_key, pub_sign_key, signed_pub_enc_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt)
          values (${keys.appId}::uuid, ${keys.userId}, ${keys.publicEncKey}, ${keys.publicSignKey}, ${keys.signedPublicEncKey}, ${keys.encPrivateEncKey}, ${keys.encPrivateSignKey}, ${keys.keyDerivationSalt})"""
      .update.run.transact(xa).void

  override def updatePrivateKeys(appId: String, userId: String, encPrivateEncKey: String, encPrivateSignKey: String): IO[Unit] =
    sql"""update user_keys set enc_priv_enc_key=$encPrivateEncKey, enc_priv_sign_key=$encPrivateSignKey
          where app_id=$appId::uuid and user_id=$userId"""
      .update.run.transact(xa).flatMap(DbUtil.ensureUpdatedOne)

  override def updatePrivateKeysAndSalt(appId: String, userId: String, encPrivateEncKey: String, encPrivateSignKey: String, keyDerivationSalt: String): IO[Unit] =
    sql"""update user_keys set enc_priv_enc_key=$encPrivateEncKey, enc_priv_sign_key=$encPrivateSignKey, key_deriv_salt=$keyDerivationSalt
          where app_id=$appId::uuid and user_id=$userId"""
      .update.run.transact(xa).flatMap(DbUtil.ensureUpdatedOne)
}
