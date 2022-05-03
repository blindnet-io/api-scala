package io.blindnet.backend
package models

import java.time.Instant

case class Message(
  id: Long,
  appId: String,
  senderId: String,
  senderDeviceId: String,
  recipientId: String,
  recipientDeviceId: String,
  data: String,
  dhKey: String,
  timeSent: Instant,
  timeDelivered: Option[Instant] = None,
  timeRead: Option[Instant] = None,
)

trait MessageRepository[F[_]] {
  def findById(appId: String, id: Long): F[Option[Message]]
  def findAllIdsByRecipient(appId: String, recipientId: String, deviceId: String): F[List[Long]]
  def insert(message: Message): F[Unit]
}
