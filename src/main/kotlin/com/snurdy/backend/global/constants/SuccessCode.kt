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
    OK(HttpStatus.OK, "COMMON_S200", "요청이 성공했습니다"),
    CREATED(HttpStatus.CREATED, "COMMON_S201", "생성되었습니다")
}

enum class AuthSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    LOGIN(HttpStatus.OK, "AUTH_S001", "로그인에 성공했습니다"),
    LOGOUT(HttpStatus.OK, "AUTH_S002", "로그아웃에 성공했습니다"),
    SIGNUP(HttpStatus.CREATED, "AUTH_S003", "회원가입에 성공했습니다"),
    REISSUE(HttpStatus.OK, "AUTH_S004", "토큰 재발급에 성공했습니다"),
    MERGE(HttpStatus.OK, "AUTH_S005", "데이터 병합에 성공했습니다")
}

enum class UserSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    WITHDRAW(HttpStatus.OK, "USER_S002", "회원탈퇴에 성공했습니다"),
    GET_STREAK(HttpStatus.OK, "USER_S010", "스트릭 조회에 성공했습니다"),
    GET_PROFILE(HttpStatus.OK, "USER_S011", "프로필 조회에 성공했습니다"),
    UPDATE_PROFILE(HttpStatus.OK, "USER_S012", "프로필을 수정했습니다"),
    UPDATE_MARKETING_CONSENT(HttpStatus.OK, "USER_S013", "마케팅 수신 동의를 변경했습니다")
}

enum class MigrationSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    START(HttpStatus.CREATED, "MIGRATION_S001", "마이그레이션이 시작되었습니다"),
    UPLOAD(HttpStatus.OK, "MIGRATION_S002", "업로드가 완료되었습니다"),
    FINALIZE(HttpStatus.OK, "MIGRATION_S003", "마이그레이션이 완료되었습니다"),
    STATUS(HttpStatus.OK, "MIGRATION_S004", "마이그레이션 상태 조회에 성공했습니다")
}

enum class LearningSuccess (
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    SEARCH(HttpStatus.OK, "LEARNING_S001", "조회에 성공했습니다")
}

enum class DictionarySuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    SEARCH(HttpStatus.OK, "DICTIONARY_S001", "조회에 성공했습니다")
}

enum class AdminSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    CREATE_CHANGE_REQUEST(HttpStatus.CREATED, "ADMIN_S010", "변경 요청 생성에 성공했습니다"),
    VALIDATE_CHANGE_REQUEST(HttpStatus.OK, "ADMIN_S011", "변경 요청 검증에 성공했습니다"),
    APPLY_CHANGE_REQUEST(HttpStatus.OK, "ADMIN_S012", "변경 요청 반영에 성공했습니다"),
    CANCEL_CHANGE_REQUEST(HttpStatus.OK, "ADMIN_S013", "변경 요청 취소에 성공했습니다"),
    ROLLBACK_CHANGE_REQUEST(HttpStatus.OK, "ADMIN_S014", "변경 요청 롤백에 성공했습니다")
}

enum class VersionSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    CHECK(HttpStatus.OK, "VERSION_S001", "업데이트 확인이 완료되었습니다");
}
