package com.snurdy.backend.global.common.constants

import org.springframework.http.HttpStatus

enum class ErrorType(
    val httpStatus: HttpStatus,
    val message: String
) {
    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다"),
    INVALID_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 타입입니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다"),

    // OAuth
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "카카오 API 오류가 발생했습니다"),
    APPLE_API_ERROR(HttpStatus.BAD_GATEWAY, "애플 API 오류가 발생했습니다"),

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다");

    val httpStatusCode: Int
        get() = httpStatus.value()
}