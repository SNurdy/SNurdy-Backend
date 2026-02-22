package com.snurdy.backend.global.constants

import org.springframework.http.HttpStatus

interface ErrorCode {
    val status: HttpStatus
    val code: String
    val message: String
}

// 공통 에러
enum class CommonError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "접근 권한이 없습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405", "지원하지 않는 HTTP 메서드입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 오류가 발생했습니다"),
    BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "COMMON_502", "외부 API 오류가 발생했습니다")

}


// 인증 관련 에러
enum class AuthError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "만료된 토큰입니다"),
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_003", "카카오 API 오류가 발생했습니다"),
    APPLE_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_004", "애플 API 오류가 발생했습니다"),
    GOOGLE_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_005", "구글 API 오류가 발생했습니다")
}

// 유저 관련 에러
enum class UserError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다"),
    DUPLICATED(HttpStatus.CONFLICT, "USER_002", "이미 존재하는 사용자입니다")
}

enum class CatalogError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    GLOSSARY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_001", "단어장을 찾을 수 없습니다"),
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_002", "단어를 찾을 수 없습니다"),
    INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "CATALOG_003", "키워드는 2자 이상 50자 이하여야 합니다")
}

enum class AdminError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    CHANGE_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_001", "변경 요청을 찾을 수 없습니다"),
    INVALID_CHANGE_SET_STATUS(HttpStatus.CONFLICT, "ADMIN_002", "현재 상태에서는 요청을 처리할 수 없습니다"),
    MISSING_CHANGE_SET_ITEMS(HttpStatus.BAD_REQUEST, "ADMIN_003", "변경 요청에 처리할 항목이 없습니다"),
    CHANGE_SET_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_004", "변경 항목을 찾을 수 없습니다"),
    IMMUTABLE_CHANGE_SET(HttpStatus.CONFLICT, "ADMIN_005", "현재 상태에서는 변경 항목을 수정할 수 없습니다"),
    INVALID_CHANGE_SET_ITEM_REQUEST(HttpStatus.BAD_REQUEST, "ADMIN_006", "변경 항목 요청 값이 유효하지 않습니다")
}
