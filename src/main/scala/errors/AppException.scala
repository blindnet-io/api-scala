package io.blindnet.backend
package errors

abstract class AppException(message: String = null, cause: Throwable = null) extends Exception(message, cause)

class BadRequestException(message: String = null) extends AppException(message)
class NotFoundException(message: String = null) extends AppException(message)
