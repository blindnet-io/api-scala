package io.blindnet.backend
package models

case class App(
  id: String,
  publicKey: String,
  name: String,
)

trait AppRepository[F[_]] {
  def findById(id: String): F[Option[App]]
  def insert(app: App): F[Unit]
}
