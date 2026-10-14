package com.snurdy.backend.domain.test.common.dto

// 정답 / 입력 변형 enum 의 sampling / derive / 분류 helper

fun TestType.requiresAbbreviation(): Boolean = when (this) {
    TestType.WRITE_ABBREVIATION_FROM_TERM,
    TestType.MC_TERM_FROM_ABBREVIATION -> true
    else -> false
}

fun TestType.isMultipleChoice(): Boolean = when (this) {
    TestType.MC_MEANING_FROM_TERM,
    TestType.MC_TERM_FROM_ABBREVIATION,
    TestType.MC_TERM_FROM_MEANING -> true
    else -> false
}

// question 필드 derive — testType 만으로 정답/노출 단어 결정 가능
fun TestType.deriveQuestion(spelling: String, meaning: String, abbreviation: String?): String = when (this) {
    TestType.WRITE_TERM_FROM_MEANING -> meaning
    TestType.WRITE_MEANING_FROM_TERM -> spelling
    TestType.WRITE_ABBREVIATION_FROM_TERM -> spelling
    TestType.LISTEN_AND_WRITE -> spelling
    TestType.MC_MEANING_FROM_TERM -> spelling
    TestType.MC_TERM_FROM_ABBREVIATION -> requireNotBlank(abbreviation, this, "question")
    TestType.MC_TERM_FROM_MEANING -> meaning
}

fun TestType.deriveAnswer(spelling: String, meaning: String, abbreviation: String?): String = when (this) {
    TestType.WRITE_TERM_FROM_MEANING -> spelling
    TestType.WRITE_MEANING_FROM_TERM -> meaning
    TestType.WRITE_ABBREVIATION_FROM_TERM -> requireNotBlank(abbreviation, this, "answer")
    TestType.LISTEN_AND_WRITE -> spelling
    TestType.MC_MEANING_FROM_TERM -> meaning
    TestType.MC_TERM_FROM_ABBREVIATION -> spelling
    TestType.MC_TERM_FROM_MEANING -> spelling
}

// 다지선다 카드의 정답 / 보기 텍스트가 추출되는 단어 필드 — distractor sampling 에서 사용
fun TestType.multipleChoiceFieldOf(spelling: String, meaning: String, abbreviation: String?): String? = when (this) {
    TestType.MC_MEANING_FROM_TERM -> meaning
    TestType.MC_TERM_FROM_ABBREVIATION -> spelling
    TestType.MC_TERM_FROM_MEANING -> spelling
    else -> null
}

private fun requireNotBlank(value: String?, testType: TestType, fieldKind: String): String =
    value?.takeIf { it.isNotBlank() }
        ?: error("$testType $fieldKind requires non-blank abbreviation but term has none")
