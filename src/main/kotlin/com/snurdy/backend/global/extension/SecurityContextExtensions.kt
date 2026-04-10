package com.snurdy.backend.global.extension

import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

fun currentUserId(): UUID {
    return when (val principal = SecurityContextHolder.getContext().authentication?.principal) {
        is UUID -> principal
        is String -> principal.toUUIDOrNull() ?: throw ApiException(CommonError.UNAUTHORIZED)
        else -> throw ApiException(CommonError.UNAUTHORIZED)
    }
}

private fun String.toUUIDOrNull(): UUID? =
    runCatching { UUID.fromString(this) }.getOrNull()
