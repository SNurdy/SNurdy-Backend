package com.snurdy.backend.global.exception;

import com.snurdy.backend.global.common.constants.ErrorType;
import com.snurdy.backend.global.common.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
		return ResponseEntity
			.status(e.getErrorType().getHttpStatus())
			.body(ApiResponse.error(e.getErrorType()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException e) {
		return ResponseEntity
			.status(ErrorType.BAD_REQUEST.getHttpStatus())
			.body(ApiResponse.error(ErrorType.BAD_REQUEST, e));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
		return ResponseEntity
			.status(ErrorType.INTERNAL_SERVER_ERROR.getHttpStatus())
			.body(ApiResponse.error(ErrorType.INTERNAL_SERVER_ERROR, e));
	}
}