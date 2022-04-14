package io.blindnet.backend
package models

import java.time.Instant

case class Message(
  id: Long,
  appId: String,
  senderId: String,
  recipientId: String,
  data: String,
  timeSent: Instant,
)

trait MessageRepository[F[_]] {
  def findById(appId: String, id: Long): F[Option[Message]]
  def insert(message: Message): F[Unit]
}
