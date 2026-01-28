package com.snurdy.backend.global.exception;

import com.snurdy.backend.global.common.constants.ErrorType;
import com.snurdy.backend.global.common.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
		log.warn("커스텀 예외 발생: {}", e.getErrorType().getMessage());

		return ResponseEntity
			.status(e.getErrorType().getHttpStatus())
			.body(ApiResponse.error(e.getErrorType()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
		MethodArgumentNotValidException e) {

		log.warn("입력값 검증 실패: {}", e.getBindingResult());

		Map<String, String> errors = new HashMap<>();
		e.getBindingResult().getAllErrors().forEach(error -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});

		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(ErrorType.BAD_REQUEST, errors));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
		HttpRequestMethodNotSupportedException e) {

		log.warn("지원하지 않는 HTTP 메서드 요청: {} ({})", e.getMethod(), e.getMessage());

		return ResponseEntity
			.status(405)
			.body(ApiResponse.error(ErrorType.BAD_REQUEST));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
		MethodArgumentTypeMismatchException e) {

		log.warn("타입 변환 실패: {} -> {}", e.getValue(), e.getRequiredType());

		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(ErrorType.BAD_REQUEST));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleJsonParseException(
		HttpMessageNotReadableException e) {

		log.warn("JSON 파싱 실패: {}", e.getMessage());

		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(ErrorType.BAD_REQUEST));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
		IllegalArgumentException e) {

		log.warn("잘못된 인자 전달: {}", e.getMessage());

		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(ErrorType.BAD_REQUEST));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		log.error("예상치 못한 서버 오류 발생", e);

		return ResponseEntity
			.status(ErrorType.INTERNAL_SERVER_ERROR.getHttpStatus())
			.body(ApiResponse.error(ErrorType.INTERNAL_SERVER_ERROR));
	}
}