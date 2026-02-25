package com.snurdy.backend.domain.admin.workflow.service.model

enum class ValidationCode(
    val code: String,
    val messageTemplate: String
) {
    EMPTY_PAYLOAD("VALIDATION_EMPTY_PAYLOAD", "변경 데이터가 없습니다"),
    INVALID_ENTITY_PK("VALIDATION_INVALID_ENTITY_PK", "대상 %s ID가 유효하지 않습니다"),

    SPELLING_REQUIRED("VALIDATION_SPELLING_REQUIRED", "spelling은 필수입니다"),
    SPELLING_TOO_LONG("VALIDATION_SPELLING_TOO_LONG", "spelling은 %d자 이하여야 합니다"),
    MEANING_REQUIRED("VALIDATION_MEANING_REQUIRED", "meaning은 필수입니다"),
    MEANING_TOO_LONG("VALIDATION_MEANING_TOO_LONG", "meaning은 %d자 이하여야 합니다"),
    ABBREVIATION_TOO_LONG("VALIDATION_ABBREVIATION_TOO_LONG", "abbreviation은 %d자 이하여야 합니다"),

    GLOSSARY_REQUIRED("VALIDATION_GLOSSARY_REQUIRED", "단어 생성 시 단어장 선택은 필수입니다"),
    GLOSSARY_NOT_FOUND("VALIDATION_GLOSSARY_NOT_FOUND", "단어장을 찾을 수 없습니다 (ID=%d)"),
    TERM_NOT_FOUND("VALIDATION_TERM_NOT_FOUND", "대상 단어를 찾을 수 없습니다 (ID=%d)"),
    MORPHEME_NOT_FOUND("VALIDATION_MORPHEME_NOT_FOUND", "대상 어근어미를 찾을 수 없습니다 (ID=%d)"),
    MORPHEME_ID_INVALID("VALIDATION_MORPHEME_ID_INVALID", "어근어미 ID는 양수여야 합니다"),
    MORPHEME_ID_DUPLICATE("VALIDATION_MORPHEME_ID_DUPLICATE", "중복된 어근어미 ID가 있습니다"),
    MORPHEME_ID_NOT_FOUND("VALIDATION_MORPHEME_ID_NOT_FOUND", "존재하지 않는 어근어미: %s"),
    UNSUPPORTED_OPERATION("VALIDATION_UNSUPPORTED_OPERATION", "%s은(는) %s만 지원합니다"),

    TERM_SPELLING_DUPLICATE("WARNING_TERM_SPELLING_DUPLICATE", "동일 spelling 단어 존재: '%s' (ID=%d, 뜻=%s)"),
    MORPHEME_SPELLING_DUPLICATE("WARNING_MORPHEME_SPELLING_DUPLICATE", "동일 spelling 어근어미 존재: '%s' (ID=%d, 뜻=%s)");

    fun format(vararg args: Any?): String =
        if (args.isEmpty()) messageTemplate
        else messageTemplate.format(*args)
}