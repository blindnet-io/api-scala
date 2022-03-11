package io.blindnet.backend
package models

case class DocumentKey(
  appId: String,
  documentId: String,
  userId: String,
  encSymmetricKey: String,
)

trait DocumentKeyRepository[F[_]] {
  def findAllByDocument(appId: String, docId: String): F[List[DocumentKey]]
  def insert(key: DocumentKey): F[Unit]
}
