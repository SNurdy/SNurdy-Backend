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

// 값 없음의 표현을 null 하나로 통일하려고 blank 를 null 로 정규화
fun String?.normalizeToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
