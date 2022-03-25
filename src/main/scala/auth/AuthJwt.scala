package io.blindnet.backend
package auth

import models.*

import cats.effect.*
import io.circe.*

sealed trait AuthJwt {
  val appId: String

  def asAnyUser: IO[AnyUserJwt] = if isInstanceOf[AnyUserJwt] then IO.pure(asInstanceOf[AnyUserJwt]) else IO.raiseError(AuthException("Wrong JWT type"))
  def asUser: IO[UserJwt] = if isInstanceOf[UserJwt] then IO.pure(asInstanceOf[UserJwt]) else IO.raiseError(AuthException("Wrong JWT type"))
  def asTempUser: IO[TempUserJwt] = if isInstanceOf[TempUserJwt] then IO.pure(asInstanceOf[TempUserJwt]) else IO.raiseError(AuthException("Wrong JWT type"))
  def asClient: IO[ClientJwt] = if isInstanceOf[ClientJwt] then IO.pure(asInstanceOf[ClientJwt]) else IO.raiseError(AuthException("Wrong JWT type"))
}

sealed trait AnyUserJwt extends AuthJwt {
  /**
   * If applied on a tempuser JWT, checks whether it contains the given user IDs or if its group contains all provided user IDs.
   * On a user JWT, does nothing.
   * @param userIds User IDs to check
   * @return IO of Unit if check succeeded
   * @throws AuthException if check failed
   */
  def containsUserIds(userIds: List[String], userRepo: UserRepository[IO]): IO[Unit] = this match {
    case uJwt: UserJwt => IO.unit
    case tuJwt: TempUserJwt =>
      if tuJwt.userIds.containsSlice(userIds) then IO.unit
      else tuJwt.groupId match {
        case Some(groupId) => userRepo.countByIdsOutsideGroup(groupId, userIds).flatMap {
          wrongUsers => if wrongUsers == 0 then IO.unit else IO.raiseError(AuthException("Temporary token lacks permission for some users"))
        }
        case None => IO.raiseError(AuthException("Temporary token lacks permission for some users"))
      }
  }
  
  def containsGroup(groupId: String): Boolean = this match {
    case uJwt: UserJwt => true
    case tuJwt: TempUserJwt => tuJwt.groupId.contains(groupId)
  }
}

case class UserJwt(appId: String, userId: String, groupId: String) extends AnyUserJwt
implicit val dUserAuthJwt: Decoder[UserJwt] = Decoder.forProduct3("app", "uid", "gid")(UserJwt.apply)

case class TempUserJwt(appId: String, groupId: Option[String], tokenId: String, userIds: Seq[String]) extends AnyUserJwt
implicit val dTempUserAuthJwt: Decoder[TempUserJwt] = Decoder.forProduct4("app", "gid", "tid", "uids")(TempUserJwt.apply)

case class ClientJwt(appId: String, tokenId: String) extends AuthJwt
implicit val dClientAuthJwt: Decoder[ClientJwt] = Decoder.forProduct2("app", "tid")(ClientJwt.apply)
