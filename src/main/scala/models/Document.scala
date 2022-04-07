package io.blindnet.backend
package models

case class Document(
  appId: String,
  id: String,
)

trait DocumentRepository[F[_]] {
  def findAllByIds(appId: String, ids: List[String]): F[List[Document]]
  def findById(appId: String, id: String): F[Option[Document]]
  def insert(doc: Document): F[Unit]
  def delete(appId: String, id: String): F[Unit]
}
