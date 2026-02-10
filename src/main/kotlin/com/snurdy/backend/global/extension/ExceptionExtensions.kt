package com.snurdy.backend.global.extension

import com.snurdy.backend.global.constants.ErrorCode
import com.snurdy.backend.global.exception.AuthenticationException
import com.snurdy.backend.global.exception.TokenExpiredException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException

// JWT 예외를 도메인 예외로 변환
fun JwtException.toDomainException(): AuthenticationException = when (this) {
    is ExpiredJwtException -> TokenExpiredException(cause = this)
    else -> AuthenticationException(cause = this)
}

inline fun <T> runCatchingJwt(block: () -> T): T =
    runCatching { block() }
        .getOrElse { e ->
            when (e) {
                is JwtException -> throw e.toDomainException()
                else -> throw e
            }
        }