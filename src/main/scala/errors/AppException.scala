package io.blindnet.backend
package errors

import cats.effect.*

import scala.util.{Failure, Success, Try}

abstract class AppException(message: String = null, cause: Throwable = null) extends Exception(message, cause)

class BadRequestException(message: String) extends AppException(message)
class NotFoundException(message: String = null) extends AppException(message)

extension[T](o: IO[Option[T]]) {
  def orNotFound: IO[T] =
    o.flatMap(opt => opt match
      case Some(value) => IO.pure(value)
      case None => IO.raiseError(NotFoundException())
    )
}

extension[T](t: Try[T]) {
  def orBadRequest(message: String): IO[T] = t match
    case Failure(_) => IO.raiseError(BadRequestException(message))
    case Success(value) => IO.pure(value)
}

extension[T](l: IO[List[T]]) {
  def ensureSize(n: Int, e: => Exception = NotFoundException()): IO[List[T]] =
    l.flatMap(list =>
      if list.size == n then IO.pure(list)
      else IO.raiseError(e)
    )
}
