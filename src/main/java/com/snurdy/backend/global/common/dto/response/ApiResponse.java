package com.snurdy.backend.global.common.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.snurdy.backend.global.common.constants.ErrorType;
import com.snurdy.backend.global.common.constants.SuccessType;

import java.time.LocalDateTime;

@JsonPropertyOrder({"code", "message", "data", "timestamp"})
public record ApiResponse<T>(
	int code,
	String message,

	@JsonInclude(JsonInclude.Include.NON_NULL)
	T data,

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	LocalDateTime timestamp
) {
	public static <T> ApiResponse<T> success(SuccessType successType, T data) {
		return new ApiResponse<>(
			successType.getHttpStatusCode(),
			successType.getMessage(),
			data,
			LocalDateTime.now()
		);
	}

	public static ApiResponse<Void> success(SuccessType successType) {
		return new ApiResponse<>(
			successType.getHttpStatusCode(),
			successType.getMessage(),
			null,
			LocalDateTime.now()
		);
	}

	public static ApiResponse<Void> error(ErrorType errorType) {
		return new ApiResponse<>(
			errorType.getHttpStatusCode(),
			errorType.getMessage(),
			null,
			LocalDateTime.now()
		);
	}

	public static <T> ApiResponse<T> error(ErrorType errorType, T data) {
		return new ApiResponse<>(
			errorType.getHttpStatusCode(),
			errorType.getMessage(),
			data,
			LocalDateTime.now()
		);
	}

	public static ApiResponse<String> error(ErrorType errorType, Exception exception) {
		return new ApiResponse<>(
			errorType.getHttpStatusCode(),
			errorType.getMessage(),
			exception.getMessage(),
			LocalDateTime.now()
		);
	}
}