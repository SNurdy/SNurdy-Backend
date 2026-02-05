package com.snurdy.backend.global.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.snurdy.backend.global.common.constants.SuccessCode
import com.snurdy.backend.global.constants.ErrorCode
import java.time.LocalDateTime

@JsonPropertyOrder("code", "message", "data", "timestamp")
data class ApiResponse<T>(
    val code: Int,
    val message: String,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val data: T? = null,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(successCode: SuccessCode, data: T): ApiResponse<T> = ApiResponse(
            code = successCode.httpStatusCode,
            message = successCode.message,
            data = data
        )

        fun success(successCode: SuccessCode): ApiResponse<Unit> = ApiResponse(
            code = successCode.httpStatusCode,
            message = successCode.message
        )

        fun error(errorCode: ErrorCode): ApiResponse<Unit> = ApiResponse(
            code = errorCode.httpStatusCode,
            message = errorCode.message
        )

        fun <T> error(errorCode: ErrorCode, data: T): ApiResponse<T> = ApiResponse(
            code = errorCode.httpStatusCode,
            message = errorCode.message,
            data = data
        )
    }
}