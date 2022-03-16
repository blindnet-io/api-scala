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
  def findAllByUser(appId: String, userId: String): F[List[DocumentKey]]
  def findByDocumentAndUser(appId: String, docId: String, userId: String): F[Option[DocumentKey]]
  def insert(key: DocumentKey): F[Unit]
  def updateOne(key: DocumentKey): F[Unit]
}
