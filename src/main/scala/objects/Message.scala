package io.blindnet.backend
package objects

case class SendMessagePayload(
  recipientID: String,
  recipientDeviceID: String,
  senderDeviceID: String,
  message: String,
  dhKey: String,
  senderKeys: SenderKeys,
  timestamp: String,
)

case class SenderKeys(
  publicIk: String,
  publicEk: String,
)

case class MessageResponse(
  id: String,
  senderID: String,
  recipientID: String,
  senderDeviceID: String,
  recipientDeviceID: String,
  messageContent: String,
  dhKey: String,
  timeSent: String,
  timeDelivered: Option[String],
  timeRead: Option[String],
  messageSenderKeys: MessageSenderKeys,
)
object MessageResponse {
  def apply(message: models.Message): MessageResponse = new MessageResponse(
    message.id.toString,
    message.senderId, message.recipientId,
    message.senderDeviceId, message.recipientDeviceId,
    message.data, message.dhKey,
    message.timeSent.toString, message.timeDelivered.map(_.toString), message.timeRead.map(_.toString),
    MessageSenderKeys(message)
  )
}

case class MessageSenderKeys(
  publicIk: String,
  publicEk: String,
  messageID: String,
)
object MessageSenderKeys {
  def apply(message: models.Message): MessageSenderKeys = new MessageSenderKeys(
    message.publicIk, message.publicEk,
    message.id.toString
  )
}
