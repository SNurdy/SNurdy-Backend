package com.snurdy.backend.global.common.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.snurdy.backend.global.common.constants.ErrorType
import com.snurdy.backend.global.common.constants.SuccessType
import java.time.LocalDateTime

@JsonPropertyOrder("code", "message", "data", "timestamp")
data class ApiResponse<T>(
    val code: Int,
    val message: String,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val data: T?,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(successType: SuccessType, data: T): ApiResponse<T> {
            return ApiResponse(
                code = successType.httpStatusCode,
                message = successType.message,
                data = data,
                timestamp = LocalDateTime.now()
            )
        }

        fun success(successType: SuccessType): ApiResponse<Unit> {
            return ApiResponse(
                code = successType.httpStatusCode,
                message = successType.message,
                data = null,
                timestamp = LocalDateTime.now()
            )
        }

        fun error(errorType: ErrorType): ApiResponse<Unit> {
            return ApiResponse(
                code = errorType.httpStatusCode,
                message = errorType.message,
                data = null,
                timestamp = LocalDateTime.now()
            )
        }

        fun <T> error(errorType: ErrorType, data: T): ApiResponse<T> {
            return ApiResponse(
                code = errorType.httpStatusCode,
                message = errorType.message,
                data = data,
                timestamp = LocalDateTime.now()
            )
        }

        fun error(errorType: ErrorType, exception: Exception): ApiResponse<String> {
            return ApiResponse(
                code = errorType.httpStatusCode,
                message = errorType.message,
                data = exception.message,
                timestamp = LocalDateTime.now()
            )
        }
    }
}