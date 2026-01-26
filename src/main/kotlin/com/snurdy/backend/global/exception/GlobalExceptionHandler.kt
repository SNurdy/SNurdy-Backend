package com.snurdy.backend.global.exception

import com.snurdy.backend.global.common.constants.ErrorType
import com.snurdy.backend.global.common.dto.response.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ApiResponse<Unit>> { // Unit? -> Unit으로 변경
        return ResponseEntity
            .status(e.errorType.httpStatus)
            .body(ApiResponse.error(e.errorType))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ApiResponse<String>> {
        return ResponseEntity
            .status(ErrorType.BAD_REQUEST.httpStatus)
            .body(ApiResponse.error(ErrorType.BAD_REQUEST, e))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<String>> {
        return ResponseEntity
            .status(ErrorType.INTERNAL_SERVER_ERROR.httpStatus)
            .body(ApiResponse.error(ErrorType.INTERNAL_SERVER_ERROR, e))
    }
}