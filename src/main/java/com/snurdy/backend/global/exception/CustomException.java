package com.snurdy.backend.global.exception;

import com.snurdy.backend.global.common.constants.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CustomException extends RuntimeException {
	private final ErrorType errorType;

	@Override
	public String getMessage() {
		return errorType.getMessage();
	}
}