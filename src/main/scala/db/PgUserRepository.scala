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
  override def countByIdsOutsideGroup(appId: String, groupId: String, userIds: List[String]): IO[Long] =
    NonEmptyList.fromList(userIds) match
      case Some(nel) => (fr"select count(*) from users where app=$appId::uuid and group_id != $groupId and"
        ++ Fragments.in(fr"id", nel))
        .query[Long].unique.transact(xa)
      case None => IO.pure(0)

  override def findById(appId: String, id: String): IO[Option[User]] =
    sql"select app, id, group_id from users where app=$appId::uuid and id=$id"
      .query[User].option.transact(xa)

  override def findAllByGroup(appId: String, groupId: String): IO[List[User]] =
    sql"select app, id, group_id from users where app=$appId::uuid and group_id=$groupId"
      .query[User].to[List].transact(xa)

  override def findAllByIds(appId: String, ids: List[String]): IO[List[User]] =
    NonEmptyList.fromList(ids) match
      case Some(nel) => (fr"select app, id, group_id from users where app=$appId::uuid and"
        ++ Fragments.in(fr"id", nel))
        .query[User].to[List].transact(xa)
      case None => IO.pure(Nil)

  override def insert(user: User): IO[Unit] =
    sql"""insert into users (app, id, group_id)
          values (${user.appId}::uuid, ${user.id}, ${user.groupId})"""
      .update.run.transact(xa).void

  override def delete(appId: String, id: String): IO[Unit] =
    sql"delete from users where app=$appId::uuid and id=$id"
      .update.run.transact(xa).flatMap(DbUtil.ensureUpdatedOne)

  override def deleteAllByGroup(appId: String, groupId: String): IO[Unit] =
    sql"delete from users where app=$appId::uuid and group_id=$groupId"
      .update.run.transact(xa).flatMap(DbUtil.ensureUpdatedAtLeastOne)
}
