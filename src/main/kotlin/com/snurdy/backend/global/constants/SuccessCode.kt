package com.snurdy.backend.global.constants

import org.springframework.http.HttpStatus

interface SuccessCode {
    val status: HttpStatus
    val code: String
    val message: String
}

enum class CommonSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    OK(HttpStatus.OK, "SUCCESS", "요청이 성공했습니다"),
    CREATED(HttpStatus.CREATED, "CREATED", "생성되었습니다")
}

enum class AuthSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    LOGIN(HttpStatus.OK, "AUTH_001", "로그인에 성공했습니다"),
    LOGOUT(HttpStatus.OK, "AUTH_002", "로그아웃에 성공했습니다"),
    SIGNUP(HttpStatus.CREATED, "AUTH_003", "회원가입에 성공했습니다"),
    REISSUE(HttpStatus.OK, "AUTH_004", "토큰 재발급에 성공했습니다"),
    MERGE(HttpStatus.OK, "AUTH_005", "데이터 병합에 성공했습니다")
}

enum class LearningSuccess (
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    SEARCH(HttpStatus.OK, "LEARNING_001", "조회에 성공했습니다")
}

enum class CatalogSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    SEARCH(HttpStatus.OK, "CATALOG_001", "조회에 성공했습니다")
}

enum class AdminSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    VALIDATE(HttpStatus.OK, "ADMIN_001", "검증에 성공했습니다"),
    APPROVE(HttpStatus.OK, "ADMIN_002", "승인에 성공했습니다"),
    APPLY(HttpStatus.OK, "ADMIN_003", "적용에 성공했습니다")
}
