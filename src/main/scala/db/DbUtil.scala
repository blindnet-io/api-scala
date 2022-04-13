package io.blindnet.backend
package db

import errors.NotFoundException

import cats.effect.IO
import cats.implicits.*
import cats.Reducible
import doobie.*
import doobie.implicits.*

object DbUtil {
  def ensureUpdatedOne(count: Int): IO[Unit] =
    if count == 1 then IO.unit else IO.raiseError(NotFoundException())

  def ensureUpdatedAtLeastOne(count: Int): IO[Unit] =
    if count >= 1 then IO.unit else IO.raiseError(NotFoundException())

  object Fragments {
    /** Returns `f IN (fs0, fs1, ...)`, casting fsn to uuid. */
    def inUuid[F[_]: Reducible, A: util.Put](f: Fragment, fs: F[A]): Fragment =
      fs.toList.map(a => fr0"$a::uuid").foldSmash1(f ++ fr0"IN (", fr",", fr")")
  }
}
