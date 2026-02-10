package com.snurdy.backend.global.exception

import com.snurdy.backend.global.constants.ErrorCode

sealed class DomainException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

open class AuthenticationException(
    errorCode: ErrorCode = ErrorCode.INVALID_TOKEN,
    cause: Throwable? = null
) : DomainException(errorCode, cause = cause)

class TokenExpiredException(
    cause: Throwable? = null
) : AuthenticationException(ErrorCode.EXPIRED_TOKEN, cause = cause)

class ExternalApiException(
    errorCode: ErrorCode,
    cause: Throwable? = null
) : DomainException(errorCode, cause = cause)

class BusinessException(
    errorCode: ErrorCode
) : DomainException(errorCode)

class ResourceNotFoundException(
    errorCode: ErrorCode = ErrorCode.NOT_FOUND
) : DomainException(errorCode)