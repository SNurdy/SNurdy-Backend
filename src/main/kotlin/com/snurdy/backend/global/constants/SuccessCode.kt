package com.snurdy.backend.global.common.constants

import org.springframework.http.HttpStatus

enum class SuccessCode(
    val httpStatus: HttpStatus,
    val message: String
) {
    // Auth
    LOGIN_SUCCESS(HttpStatus.OK, "로그인에 성공했습니다"),
    LOGOUT_SUCCESS(HttpStatus.OK, "로그아웃에 성공했습니다"),
    SIGNUP_SUCCESS(HttpStatus.CREATED, "회원가입에 성공했습니다"),
    REISSUE_SUCCESS(HttpStatus.OK, "토큰 재발급에 성공했습니다"),
    MERGE_SUCCESS(HttpStatus.OK, "데이터 병합에 성공했습니다"),

    // Common
    SEARCH_SUCCESS(HttpStatus.OK, "조회에 성공했습니다");

    val httpStatusCode: Int
        get() = httpStatus.value()
}