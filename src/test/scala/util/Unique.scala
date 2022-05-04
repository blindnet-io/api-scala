package io.blindnet.backend
package util

import java.util.UUID

case class Unique[T](
  data: T,
  id: String = UUID.randomUUID().toString
) {
  def get: T = data
}

trait ToUnique[T] {
  def unique: Unique[T] = Unique(this.asInstanceOf[T])
}
