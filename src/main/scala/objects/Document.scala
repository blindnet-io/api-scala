package io.blindnet.backend
package objects

type CreateDocumentPayload = List[CreateDocumentItem]
case class CreateDocumentItem(
  userID: String,
  encryptedSymmetricKey: String
)

type AddUserKeysPayload = List[AddUserKeysItem]
case class AddUserKeysItem(
  documentID: String,
  encryptedSymmetricKey: String
)

case class GetAllDocsAndKeysResponseItem(
  documentID: String,
  encryptedSymmetricKey: String
)

case class GetDocsAndKeysPayload(
  data_ids: List[String]
)
