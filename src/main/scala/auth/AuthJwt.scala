package io.blindnet.backend
package auth

import errors.*
import models.*

import cats.effect.*
import io.circe.*

sealed trait AuthJwt {
  val appId: String

  def asAnyUser: IO[AnyUserJwt] = as(classOf[AnyUserJwt])
  def asUser: IO[UserJwt] = as(classOf[UserJwt])
  def asUserNoCheck: IO[UserJwt] = asNoCheck(classOf[UserJwt])
  def asTempUser: IO[TempUserJwt] = as(classOf[TempUserJwt])
  def asClient: IO[ClientJwt] = as(classOf[ClientJwt])

  private def as[T <: AuthJwt](cl: Class[T]): IO[T] = asNoCheck(cl).flatMap(t => t.check().as(t))
  private def asNoCheck[T](cl: Class[T]): IO[T] =
    if cl.isInstance(this) then IO.pure(asInstanceOf[T]) else IO.raiseError(AuthException("Wrong JWT type"))

  protected def check(): IO[Unit] = IO.unit
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
        case Some(groupId) => userRepo.countByIdsOutsideGroup(tuJwt.appId, groupId, userIds).flatMap {
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

case class UserJwt(appId: String, userId: String, groupId: String, exists: Boolean = false) extends AnyUserJwt {
  override protected def check(): IO[Unit] =
    if exists then IO.unit else IO.raiseError(BadRequestException("Auth user does not exist"))
}
object UserJwt {
  def apply(appId: String, userId: String, groupId: String): UserJwt = new UserJwt(appId, userId, groupId)
}
implicit val dUserAuthJwt: Decoder[UserJwt] = Decoder.forProduct3("app", "uid", "gid")(UserJwt.apply)

case class TempUserJwt(appId: String, groupId: Option[String], tokenId: String, userIds: List[String]) extends AnyUserJwt
implicit val dTempUserAuthJwt: Decoder[TempUserJwt] = (c: HCursor) => 
  if c.downField("gid").succeeded
  then for {
      app <- c.downField("app").as[String]
      gid <- c.downField("gid").as[String]
      tid <- c.downField("tid").as[String]
    } yield TempUserJwt(app, Some(gid), tid, Nil)
  else for {
    app <- c.downField("app").as[String]
    uids <- c.downField("uids").as[List[String]]
    tid <- c.downField("tid").as[String]
  } yield TempUserJwt(app, None, tid, uids)

case class ClientJwt(appId: String, tokenId: String) extends AuthJwt
implicit val dClientAuthJwt: Decoder[ClientJwt] = Decoder.forProduct2("app", "tid")(ClientJwt.apply)
