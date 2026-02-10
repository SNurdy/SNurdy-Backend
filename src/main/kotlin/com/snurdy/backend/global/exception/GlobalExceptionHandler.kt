package com.snurdy.backend.global.exception

import com.snurdy.backend.global.dto.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        e: ApiException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        log.warn("[{}] {} {} - {}", e.error.code, request.method, request.requestURI, e.responseMessage)

        return ResponseEntity
            .status(e.error.status)
            .body(ApiResponse.fail(e.error, e.responseMessage))  // responseMessage 사용
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val apiException = e.toApiException()

        if (apiException.error.status.is5xxServerError) {
            log.error("[{}] {} {}", apiException.error.code, request.method, request.requestURI, e)
        } else {
            log.warn("[{}] {} {} - {}", apiException.error.code, request.method, request.requestURI, apiException.responseMessage)
        }

        return ResponseEntity
            .status(apiException.error.status)
            .body(ApiResponse.fail(apiException.error, apiException.responseMessage))
    }
}