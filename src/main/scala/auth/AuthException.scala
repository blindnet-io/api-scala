package io.blindnet.backend
package auth

import errors.AppException

class AuthException(message: String, cause: Throwable = null) extends AppException(message, cause)
