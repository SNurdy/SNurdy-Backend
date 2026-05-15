package com.snurdy.backend.global.extension

import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.exception.ApiException

private const val BEARER_PREFIX = "Bearer "

fun String.extractBearerToken(): String {
    val trimmed = trim()
    if (!trimmed.startsWith(BEARER_PREFIX, ignoreCase = true)) {
        throw ApiException(AuthError.INVALID_TOKEN)
    }
    return trimmed.substring(BEARER_PREFIX.length).trim()
}
