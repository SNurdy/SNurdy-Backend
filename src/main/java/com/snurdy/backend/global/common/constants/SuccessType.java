package com.snurdy.backend.global.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessType {
	// Auth
	LOGIN_SUCCESS(HttpStatus.OK, "로그인에 성공했습니다"),
	LOGOUT_SUCCESS(HttpStatus.NO_CONTENT, "로그아웃에 성공했습니다"),
	SIGNUP_SUCCESS(HttpStatus.CREATED, "회원가입에 성공했습니다"),
	REISSUE_SUCCESS(HttpStatus.OK, "토큰 재발급에 성공했습니다"),
	MERGE_SUCCESS(HttpStatus.OK, "데이터 병합에 성공했습니다"),

	// Common
	SEARCH_SUCCESS(HttpStatus.OK, "조회에 성공했습니다");

	private final HttpStatus httpStatus;
	private final String message;

	public int getHttpStatusCode() {
		return httpStatus.value();
	}
}