package io.blindnet.backend
package errors

abstract class AppException(message: String = null, cause: Throwable = null) extends Exception(message, cause)

class NotFoundException() extends AppException
