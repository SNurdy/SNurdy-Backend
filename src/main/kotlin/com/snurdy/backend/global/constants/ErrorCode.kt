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
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_E400", "잘못된 요청입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_E401", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_E403", "접근 권한이 없습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_E404", "리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_E405", "지원하지 않는 HTTP 메서드입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_E500", "서버 오류가 발생했습니다"),
    BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "COMMON_E502", "외부 API 오류가 발생했습니다")

}

// 마이그레이션 에러
enum class MigrationError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    MIGRATION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "MIGRATION_E001", "이미 완료된 마이그레이션입니다"),
    MIGRATION_DATA_CONFLICT(HttpStatus.CONFLICT, "MIGRATION_E002", "서버에 이미 학습 데이터가 존재합니다"),
    MIGRATION_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MIGRATION_E003", "마이그레이션 세션을 찾을 수 없습니다"),
    MIGRATION_INVALID_STATUS(HttpStatus.CONFLICT, "MIGRATION_E004", "유효하지 않은 마이그레이션 상태입니다"),
    MIGRATION_UPLOAD_REQUIRED(HttpStatus.CONFLICT, "MIGRATION_E005", "업로드가 완료되지 않았습니다"),
    MIGRATION_INVALID_TERM_STATUS(HttpStatus.BAD_REQUEST, "MIGRATION_E006", "유효하지 않은 term progress 상태값입니다"),
    MIGRATION_INVALID_GLOSSARY_ID(HttpStatus.BAD_REQUEST, "MIGRATION_E007", "유효하지 않은 glossaryId가 포함되어 있습니다"),
    MIGRATION_INVALID_TERM_ID(HttpStatus.BAD_REQUEST, "MIGRATION_E008", "유효하지 않은 termId가 포함되어 있습니다"),
    MIGRATION_DUPLICATED_TERM_PROGRESS(HttpStatus.BAD_REQUEST, "MIGRATION_E009", "중복된 term progress 항목이 포함되어 있습니다"),
    MIGRATION_DUPLICATED_GLOSSARY_PROGRESS(HttpStatus.BAD_REQUEST, "MIGRATION_E010", "중복된 glossary progress 항목이 포함되어 있습니다"),
    MIGRATION_PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "MIGRATION_E011", "payload가 10MB를 초과할 수 없습니다"),
    MIGRATION_INVALID_GLOSSARY_TERM_PAIR(HttpStatus.BAD_REQUEST, "MIGRATION_E012", "용어장과 용어의 조합이 유효하지 않습니다"),
    MIGRATION_DUPLICATED_BOOKMARK(HttpStatus.BAD_REQUEST, "MIGRATION_E013", "중복된 bookmark 항목이 포함되어 있습니다"),
    MIGRATION_DUPLICATED_DAILY_PROGRESS(HttpStatus.BAD_REQUEST, "MIGRATION_E014", "중복된 daily progress 항목이 포함되어 있습니다")
}

// 인증 관련 에러
enum class AuthError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_E001", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_E002", "만료된 토큰입니다"),
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_E003", "카카오 API 오류가 발생했습니다"),
    APPLE_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_E004", "애플 API 오류가 발생했습니다"),
    GOOGLE_API_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_E005", "구글 API 오류가 발생했습니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_E010", "이메일 또는 비밀번호가 올바르지 않습니다")
}

// 유저 관련 에러
enum class UserError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "USER_E001", "사용자를 찾을 수 없습니다"),
    DUPLICATED(HttpStatus.CONFLICT, "USER_E002", "이미 존재하는 사용자입니다")
}

enum class CatalogError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    GLOSSARY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_E001", "단어장을 찾을 수 없습니다"),
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_E002", "단어를 찾을 수 없습니다"),
    INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "CATALOG_E003", "키워드는 2자 이상 50자 이하여야 합니다")
}

enum class AdminError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    TERM_REQUIRED_FIELDS_MISSING(HttpStatus.BAD_REQUEST, "ADMIN_E011", "spelling과 meaning은 필수입니다"),
    INVALID_TERM_FIELD_LENGTH(HttpStatus.BAD_REQUEST, "ADMIN_E012", "용어 필드 길이가 유효하지 않습니다"),
    INVALID_WORKFLOW_OPERATION(HttpStatus.BAD_REQUEST, "ADMIN_E013", "지원하지 않는 작업 유형입니다"),
    WORKFLOW_TERM_ID_REQUIRED(HttpStatus.BAD_REQUEST, "ADMIN_E014", "수정/삭제 요청에는 termId가 필요합니다"),
    WORKFLOW_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "ADMIN_E015", "요청 상태 전이가 올바르지 않습니다"),
    WORKFLOW_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "ADMIN_E016", "검증에 실패했습니다"),
    WORKFLOW_ONLY_APPROVER_CAN_APPLY(HttpStatus.FORBIDDEN, "ADMIN_E017", "반영은 승인 권한이 있는 관리자만 가능합니다"),
    DUPLICATE_TERM_SPELLING(HttpStatus.CONFLICT, "ADMIN_E018", "이미 존재하는 단어입니다"),
    INVALID_MORPHEME_MAPPING(HttpStatus.BAD_REQUEST, "ADMIN_E019", "어근어미 매핑 값이 유효하지 않습니다"),
    MORPHEME_NOT_FOUND(HttpStatus.BAD_REQUEST, "ADMIN_E020", "존재하지 않는 어근어미가 포함되어 있습니다"),
    GLOSSARY_REQUIRED_FOR_TERM_CREATE(HttpStatus.BAD_REQUEST, "ADMIN_E021", "용어 생성 시 용어집 선택은 필수입니다"),
    MORPHEME_REQUIRED_FIELDS_MISSING(HttpStatus.BAD_REQUEST, "ADMIN_E022", "어근어미 spelling과 meaning은 필수입니다"),
    INVALID_MORPHEME_FIELD_LENGTH(HttpStatus.BAD_REQUEST, "ADMIN_E023", "어근어미 필드 길이가 유효하지 않습니다"),
    DUPLICATE_MORPHEME_SPELLING(HttpStatus.CONFLICT, "ADMIN_E024", "이미 존재하는 어근어미입니다"),
    INVALID_WORKFLOW_ENTITY_TYPE(HttpStatus.BAD_REQUEST, "ADMIN_E025", "지원하지 않는 요청 엔티티 유형입니다"),
    WORKFLOW_ROLLBACK_FAILED(HttpStatus.CONFLICT, "ADMIN_E026", "현재 상태에서 워크플로우를 롤백할 수 없습니다"),
}
