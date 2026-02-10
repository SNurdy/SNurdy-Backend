package com.snurdy.backend.global.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.snurdy.backend.global.constants.ErrorCode
import com.snurdy.backend.global.constants.SuccessCode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ApiResponse<T>(
    val success: Boolean,
    val status: Int,
    val code: String,
    val message: String,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val data: T? = null,

    val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
) {
    companion object {
        fun <T> success(successCode: SuccessCode, data: T): ApiResponse<T> =
            ApiResponse(
                success = true,
                status = successCode.status.value(),
                code = successCode.code,
                message = successCode.message,
                data = data
            )

        fun success(successCode: SuccessCode): ApiResponse<Unit> =
            ApiResponse(
                success = true,
                status = successCode.status.value(),
                code = successCode.code,
                message = successCode.message
            )

        fun fail(error: ErrorCode, message: String = error.message): ApiResponse<Unit> =
            ApiResponse(
                success = false,
                status = error.status.value(),
                code = error.code,
                message = message
            )
    }
}