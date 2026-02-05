package com.snurdy.backend.global.exception

import com.snurdy.backend.global.constants.ErrorCode
import com.snurdy.backend.global.dto.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(e: ApiException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("Business Error: [${e.errorCode}] ${e.message}")
        return ResponseEntity
            .status(e.errorCode.httpStatus)
            .body(ApiResponse.error(e.errorCode))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Map<String, String>>> {
        val errors = e.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "유효하지 않은 값입니다")
        }
        log.warn("Validation Failed: $errors")
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(ErrorCode.BAD_REQUEST, errors))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Unit>> {
        log.error("Unexpected System Error: ", e)
        return ResponseEntity
            .status(500)
            .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR))
    }
}