package io.blindnet.backend
package db

import errors.NotFoundException

import cats.effect.IO

object DbUtil {
  def ensureUpdatedOne(count: Int): IO[Unit] =
    if count == 1 then IO.unit else IO.raiseError(NotFoundException())

  def ensureUpdatedAtLeastOne(count: Int): IO[Unit] =
    if count >= 1 then IO.unit else IO.raiseError(NotFoundException())
}
