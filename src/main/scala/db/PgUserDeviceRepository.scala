package io.blindnet.backend
package db

import models.{UserDevice, UserDeviceRepository}

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*

class PgUserDeviceRepository(xa: Transactor[IO]) extends UserDeviceRepository[IO] {
  override def findById(appId: String, userId: String, id: String): IO[Option[UserDevice]] =
    sql"select app_id, user_id, id, pub_sign_key, pub_ik_id, pub_ik, pub_spk_id, pub_spk, pk_sig from user_devices where app_id=$appId::uuid and user_id=$userId and id=$id"
      .query[UserDevice].option.transact(xa)

  override def insert(device: UserDevice): IO[Unit] =
    sql"""insert into user_devices (app_id, user_id, id, pub_sign_key, pub_ik_id, pub_ik, pub_spk_id, pub_spk, pk_sig)
          values (${device.appId}::uuid, ${device.userId}, ${device.id}, ${device.signingPublicKey}, ${device.publicIkId}, ${device.publicIk}, ${device.publicSpkId}, ${device.publicSpk}, ${device.pkSig})"""
      .update.run.transact(xa).map(_ => ())
}
