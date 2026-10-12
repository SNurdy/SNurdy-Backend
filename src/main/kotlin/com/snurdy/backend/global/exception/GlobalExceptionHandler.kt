package com.snurdy.backend.global.exception

import com.snurdy.backend.global.dto.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.util.DisconnectedClientHelper

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        e: ApiException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        if (e.error.status.is5xxServerError) {
            log.error("[{}] {} {} - {}", e.error.code, request.method, request.requestURI, e.responseMessage, e)
        } else {
            log.warn("[{}] {} {} - {}", e.error.code, request.method, request.requestURI, e.responseMessage)
        }
        return ResponseEntity
            .status(e.error.status)
            .body(ApiResponse.fail(e.error, e.responseMessage))  // responseMessage 사용
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Unit>>? {
        // 클라이언트가 먼저 연결을 끊어 응답을 쓸 수 없으므로 서버 오류로 기록하지 않는다
        if (DisconnectedClientHelper.isClientDisconnectedException(e)) {
            log.debug("클라이언트 연결 종료 {} {} - {}", request.method, request.requestURI, e.message)
            return null
        }

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