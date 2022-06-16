package io.blindnet.backend
package objects

case class InitUploadResponse(
  dataId: String,
)

case class BlockUploadUrlPayload(
  dataId: String,
  chunkSize: Int
)

case class BlockUploadUrlResponse(
  authorization: String,
  date: String,
  blockId: String,
  url: String
)

case class FinishUploadPayload(
  dataId: String,
  blockIds: List[String]
)

case class FileDownloadUrlResponse(
  authorization: String,
  date: String,
  url: String
)

case class SetMetadataPayload(
  dataID: String,
  metadata: String
)
