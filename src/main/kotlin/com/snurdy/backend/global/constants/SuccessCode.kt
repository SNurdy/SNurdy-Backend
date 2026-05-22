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

enum class BookmarkSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    LIST(HttpStatus.OK, "BOOKMARK_S001", "북마크 목록 조회에 성공했습니다"),
    ADD(HttpStatus.OK, "BOOKMARK_S002", "북마크를 추가했습니다"),
    REMOVE(HttpStatus.OK, "BOOKMARK_S003", "북마크를 삭제했습니다")
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
    UPDATE_MARKETING_CONSENT(HttpStatus.OK, "USER_S013", "마케팅 수신 동의를 변경했습니다"),
    GET_DASHBOARD(HttpStatus.OK, "USER_S020", "학습 리포트 대시보드 조회에 성공했습니다")
}

enum class MigrationSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    START(HttpStatus.CREATED, "MIGRATION_S001", "마이그레이션이 시작되었습니다"),
    UPLOAD(HttpStatus.OK, "MIGRATION_S002", "업로드가 완료되었습니다"),
    FINALIZE(HttpStatus.OK, "MIGRATION_S003", "마이그레이션이 완료되었습니다"),
    STATUS(HttpStatus.OK, "MIGRATION_S004", "마이그레이션 상태 조회에 성공했습니다"),
    DECLINE(HttpStatus.OK, "MIGRATION_S005", "마이그레이션을 거부했습니다")
}

enum class LearningSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    GET_NEXT_TERMS(HttpStatus.OK, "LEARNING_S010", "학습할 다음 단어 목록 조회에 성공했습니다"),
    GET_REVIEW_TERMS(HttpStatus.OK, "LEARNING_S011", "복습 대상 단어 목록 조회에 성공했습니다"),
    SUBMIT(HttpStatus.OK, "LEARNING_S012", "학습 결과를 반영했습니다")
}

enum class CalendarSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    GET_MONTHLY(HttpStatus.OK, "CALENDAR_S001", "월별 학습 캘린더 조회에 성공했습니다")
}

enum class DictionarySuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    // SEARCH(S001) 는 이전 클라이언트 계약 보존을 위해 enum 에 남겨둔다. 신규 호출은 endpoint 별 세분화 코드를 사용한다
    SEARCH(HttpStatus.OK, "DICTIONARY_S001", "조회에 성공했습니다"),
    SEARCH_TERMS(HttpStatus.OK, "DICTIONARY_S002", "용어 검색에 성공했습니다"),
    GET_TERM_DETAIL(HttpStatus.OK, "DICTIONARY_S003", "용어 상세 조회에 성공했습니다"),
    GET_GLOSSARIES(HttpStatus.OK, "DICTIONARY_S004", "용어장 목록 조회에 성공했습니다"),
    GET_GLOSSARY_TERMS(HttpStatus.OK, "DICTIONARY_S005", "용어장 용어 목록 조회에 성공했습니다"),
    SEARCH_BY_MORPHEME(HttpStatus.OK, "DICTIONARY_S006", "어근어미 키워드 검색에 성공했습니다")
}

enum class TermMemoSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    UPSERT_MEMO(HttpStatus.OK, "TERM_MEMO_S001", "메모를 저장했습니다"),
    DELETE_MEMO(HttpStatus.OK, "TERM_MEMO_S002", "메모를 삭제했습니다")
}

enum class CustomNoteSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    CREATE_NOTE(HttpStatus.CREATED, "CUSTOM_NOTE_S001", "커스텀 단어장을 생성했습니다"),
    LIST_NOTES(HttpStatus.OK, "CUSTOM_NOTE_S002", "커스텀 단어장 목록 조회에 성공했습니다"),
    GET_NOTE(HttpStatus.OK, "CUSTOM_NOTE_S003", "커스텀 단어장 상세 조회에 성공했습니다"),
    UPDATE_NOTE(HttpStatus.OK, "CUSTOM_NOTE_S004", "커스텀 단어장을 수정했습니다"),
    DELETE_NOTE(HttpStatus.OK, "CUSTOM_NOTE_S005", "커스텀 단어장을 삭제했습니다"),
    ADD_TERMS(HttpStatus.OK, "CUSTOM_NOTE_S006", "단어를 추가했습니다"),
    DELETE_TERM(HttpStatus.OK, "CUSTOM_NOTE_S007", "단어를 삭제했습니다"),
    LIST_TERMS(HttpStatus.OK, "CUSTOM_NOTE_S008", "커스텀 단어장 단어 목록 조회에 성공했습니다"),
    DELETE_TERMS(HttpStatus.OK, "CUSTOM_NOTE_S009", "선택한 단어들을 삭제했습니다"),
    DELETE_NOTES(HttpStatus.OK, "CUSTOM_NOTE_S010", "선택한 커스텀 단어장들을 삭제했습니다")
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
    ROLLBACK_CHANGE_REQUEST(HttpStatus.OK, "ADMIN_S014", "변경 요청 롤백에 성공했습니다"),
    LIST_CHANGE_REQUESTS(HttpStatus.OK, "ADMIN_S015", "변경 요청 목록 조회에 성공했습니다"),
    GET_CHANGE_REQUEST(HttpStatus.OK, "ADMIN_S016", "변경 요청 상세 조회에 성공했습니다"),
    UPDATE_CHANGE_REQUEST(HttpStatus.OK, "ADMIN_S017", "변경 요청을 수정했습니다"),
    LIST_AUDIT_LOGS(HttpStatus.OK, "ADMIN_S020", "감사 로그 목록 조회에 성공했습니다"),
    LIST_GLOSSARIES(HttpStatus.OK, "ADMIN_S030", "용어집 목록 조회에 성공했습니다"),
    LIST_TERMS(HttpStatus.OK, "ADMIN_S031", "용어 목록 조회에 성공했습니다"),
    GET_TERM(HttpStatus.OK, "ADMIN_S032", "용어 상세 조회에 성공했습니다"),
    LIST_MORPHEMES(HttpStatus.OK, "ADMIN_S033", "어근어미 목록 조회에 성공했습니다"),
    LIST_MORPHEME_VARIANTS(HttpStatus.OK, "ADMIN_S034", "어근어미 변형 목록 조회에 성공했습니다")
}

enum class VersionSuccess(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : SuccessCode {
    CHECK(HttpStatus.OK, "VERSION_S001", "업데이트 확인이 완료되었습니다");
}
