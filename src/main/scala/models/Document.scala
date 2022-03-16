package io.blindnet.backend
package models

case class Document(
  appId: String,
  id: String,
)

trait DocumentRepository[F[_]] {
  def findById(id: String): F[Option[Document]]
  def insert(doc: Document): F[Unit]
  def delete(id: String): F[Unit]
}
