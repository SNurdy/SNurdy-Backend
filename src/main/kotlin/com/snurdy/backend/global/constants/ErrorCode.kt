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
    CONFLICT(HttpStatus.CONFLICT, "COMMON_E409", "요청이 현재 상태와 충돌합니다"),
    DUPLICATED(HttpStatus.CONFLICT, "COMMON_E410", "이미 존재하는 데이터입니다"),
    INVALID_REFERENCE(HttpStatus.BAD_REQUEST, "COMMON_E411", "참조하는 데이터가 존재하지 않습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_E500", "서버 오류가 발생했습니다"),
    BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "COMMON_E502", "외부 API 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "COMMON_E503", "일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요")

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
    MIGRATION_DUPLICATED_DAILY_PROGRESS(HttpStatus.BAD_REQUEST, "MIGRATION_E014", "중복된 daily progress 항목이 포함되어 있습니다"),
    MIGRATION_DECLINED(HttpStatus.CONFLICT, "MIGRATION_E015", "사용자가 데이터 마이그레이션을 거부했습니다"),
    MIGRATION_UPLOAD_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "MIGRATION_E016", "마이그레이션 업로드 처리 중 서버 오류가 발생했습니다"),
    MIGRATION_FINALIZE_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "MIGRATION_E017", "마이그레이션 완료 처리 중 서버 오류가 발생했습니다"),
    MIGRATION_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "MIGRATION_E018", "마이그레이션 처리 중 오류가 발생했습니다"),
    MIGRATION_SESSION_FORBIDDEN(HttpStatus.FORBIDDEN, "MIGRATION_E019", "본인의 마이그레이션 세션만 다룰 수 있습니다")
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
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_E010", "이메일 또는 비밀번호가 올바르지 않습니다"),
    LOGIN_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_E011", "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요"),
    TOKEN_OWNER_MISMATCH(HttpStatus.FORBIDDEN, "AUTH_E012", "토큰 소유자가 일치하지 않습니다")
}

// 유저 관련 에러
enum class UserError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "USER_E001", "사용자를 찾을 수 없습니다"),
    DUPLICATED(HttpStatus.CONFLICT, "USER_E002", "이미 존재하는 사용자입니다"),
    ALREADY_DELETED(HttpStatus.CONFLICT, "USER_E005", "이미 탈퇴한 사용자입니다"),
    // 토큰은 유효하지만 DB에 사용자가 없는 경우 (탈퇴 후 토큰 미만료 등)
    ACCOUNT_NOT_FOUND(HttpStatus.UNAUTHORIZED, "USER_E010", "계정 정보가 유효하지 않습니다"),
    DASHBOARD_GLOSSARY_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_E020", "해당 과를 찾을 수 없습니다")
}

enum class ProfileImageError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    REQUIRED(HttpStatus.BAD_REQUEST, "PROFILE_IMAGE_E001", "이미지 파일이 필요합니다"),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "PROFILE_IMAGE_E002", "지원하지 않는 이미지 형식입니다 (jpg, png 만 허용)"),
    TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "PROFILE_IMAGE_E003", "이미지 크기는 5MB 이하여야 합니다")
}

enum class TestError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    TEST_TYPE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "TEST_E001", "단어장이 해당 테스트 유형의 출제 조건을 충족하지 않습니다"),
    TEST_RESULT_INVALID(HttpStatus.BAD_REQUEST, "TEST_E002", "테스트 결과가 유효하지 않습니다")
}

enum class DictionaryError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    GLOSSARY_NOT_FOUND(HttpStatus.NOT_FOUND, "DICTIONARY_E001", "단어장을 찾을 수 없습니다"),
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "DICTIONARY_E002", "단어를 찾을 수 없습니다"),
    INVALID_SEARCH_KEYWORD(HttpStatus.BAD_REQUEST, "DICTIONARY_E003", "키워드는 2자 이상 50자 이하여야 합니다"),
    TERM_NOT_IN_GLOSSARY(HttpStatus.BAD_REQUEST, "DICTIONARY_E004", "해당 용어가 용어장에 속하지 않습니다"),
    INVALID_FILTER(HttpStatus.BAD_REQUEST, "DICTIONARY_E005", "filter는 all|learned|notLearned 중 하나여야 합니다"),
    MORPHEME_NOT_FOUND(HttpStatus.NOT_FOUND, "DICTIONARY_E006", "어근어미를 찾을 수 없습니다")
}

enum class TermMemoError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "TERM_MEMO_E001", "메모를 찾을 수 없습니다")
}

enum class WrongAnswerNoteError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    TERM_NOT_IN_NOTE(HttpStatus.BAD_REQUEST, "WRONG_ANSWER_NOTE_E001", "오답노트에 없는 단어입니다"),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "WRONG_ANSWER_NOTE_E002", "유효하지 않은 기간입니다")
}

enum class CustomNoteError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "CUSTOM_NOTE_E001", "커스텀 단어장을 찾을 수 없습니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "CUSTOM_NOTE_E002", "본인 소유 커스텀 단어장만 접근할 수 있습니다"),
    NOTE_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "CUSTOM_NOTE_E003", "사용자당 최대 커스텀 단어장 수를 초과했습니다"),
    TERM_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "CUSTOM_NOTE_E004", "단어장당 최대 단어 수를 초과했습니다"),
    NOT_CUSTOM_NOTE(HttpStatus.BAD_REQUEST, "CUSTOM_NOTE_E005", "커스텀 단어장이 아닙니다"),
    DUPLICATED_TITLE(HttpStatus.CONFLICT, "CUSTOM_NOTE_E006", "이미 존재하는 커스텀 단어장입니다"),
    TERM_NOT_IN_NOTE(HttpStatus.BAD_REQUEST, "CUSTOM_NOTE_E007", "해당 단어가 커스텀 단어장에 포함되어 있지 않습니다"),
    SHARE_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "CUSTOM_NOTE_E008", "유효하지 않거나 만료된 공유 코드입니다"),
    SHARE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CUSTOM_NOTE_E009", "공유 코드 발급에 실패했습니다"),
    SHARE_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "CUSTOM_NOTE_E010", "이미 발급된 공유 코드가 있습니다")
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
    WORKFLOW_MORPHEME_ID_REQUIRED(HttpStatus.BAD_REQUEST, "ADMIN_E027", "형태소 수정 요청에는 morphemeId가 필요합니다"),
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
    CHANGE_REQUEST_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "ADMIN_E028", "변경 요청 제목은 비어 있을 수 없습니다"),
    CHANGE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMIN_E029", "변경 요청을 찾을 수 없습니다"),
    CHANGE_REQUEST_INVALID_STATE(HttpStatus.INTERNAL_SERVER_ERROR, "ADMIN_E030", "변경 요청의 상태가 유효하지 않습니다"),
    WORKFLOW_PAYLOAD_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "ADMIN_E031", "변경 요청 페이로드를 파싱할 수 없습니다"),
}

enum class VersionError(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : ErrorCode {
    UNSUPPORTED_PLATFORM(HttpStatus.BAD_REQUEST, "VERSION_E001", "지원하지 않는 플랫폼입니다"),
    INVALID_VERSION_FORMAT(HttpStatus.BAD_REQUEST, "VERSION_E002", "올바르지 않은 버전 형식입니다"),
    VERSION_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "VERSION_E003", "버전 정보를 찾을 수 없습니다");
}
