package com.snurdy.backend.global.exception

import com.snurdy.backend.global.constants.ErrorCode

class ApiException(
    val error: ErrorCode,
    private val detailMessage: String? = null,
    override val cause: Throwable? = null
) : RuntimeException(detailMessage ?: error.message, cause) {

    // 실제 응답에 사용할 메시지
    val responseMessage: String
        get() = detailMessage ?: error.message
}