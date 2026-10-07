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

// 이모지가 반으로 잘려 깨지지 않도록 UTF-16 단위가 아닌 코드포인트 단위로 자름
fun String.truncateByCodePoints(limit: Int): String {
    if (codePointCount(0, length) <= limit) return this
    return substring(0, offsetByCodePoints(0, limit))
}

// 목록이나 메일 제목처럼 한 줄로 보여야 하는 곳에서 줄바꿈과 주변 공백을 공백 하나로 합침
fun String.toSingleLine(): String = trim().replace(LINE_BREAK_REGEX, " ")

private val LINE_BREAK_REGEX = Regex("\\s*\\R\\s*")