package com.snurdy.backend.domain.card.dto

import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.domain.user.customNote.repository.NoteCardTermProjection
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

@Schema(description = "과별 단어장 학습 카드 1개")
data class GlossaryCard(
    @field:Schema(description = "단어 id", example = "293")
    val termId: Long,
    @field:Schema(description = "이 카드의 테스트 유형", example = "WRITE_MEANING_FROM_TERM")
    val testType: TestType,
    @field:Schema(description = "문제 텍스트, 서버가 testType 으로 derive 한 노출 문구. LISTEN_AND_WRITE 는 클라가 화면 노출 없이 TTS 재생만", example = "arterial line")
    val question: String,
    @field:Schema(description = "다지선다 대표 정답 텍스트. MC 채점에 사용. 작성형은 answers 사용", example = "동맥선, 동맥 카테터")
    val answer: String,
    @field:Schema(description = "작성형 채점용 인정 정답 목록. 콤마 동의어를 분리한 결과로 하나라도 일치하면 정답. MC 카드는 answer 사용", example = "[\"동맥선\", \"동맥 카테터\"]")
    val answers: List<String>,
    @field:Schema(description = "다지선다 카드의 보기 3개. 작성형 / LISTEN_AND_WRITE 카드는 null", example = "[\"Hemoglobin\", \"Hematocrit\", \"Erythrocyte\"]")
    val distractors: List<String>?,
    @field:Schema(description = "영문 용어", example = "arterial line")
    val spelling: String,
    @field:Schema(description = "한글 의미", example = "동맥선, 동맥 카테터")
    val meaning: String,
    @field:Schema(description = "약어. 없으면 null", example = "A-Line")
    val abbreviation: String?,
    @field:Schema(description = "상세 설명. 없으면 null", example = "지속적인 혈압 감시 및 동맥혈 채취를 위해 동맥 내에 삽입하는 카테터입니다.")
    val explanation: String?,
    @field:Schema(description = "현재 학습 중인 단어장 안에서 사용자가 북마크 했는지 여부", example = "false")
    val bookmarked: Boolean,
    @field:Schema(description = "어근/어미 분석 + 형광펜 하이라이팅 범위. 색은 클라이언트가 order 순서대로 매핑")
    val morphemes: List<OrderedMorpheme>
)

@Schema(description = "과별 단어장 학습 세트 단어 1개")
data class GlossaryStudyTerm(
    @field:Schema(description = "단어 id", example = "293")
    val termId: Long,
    @field:Schema(description = "영문 용어", example = "arterial line")
    val spelling: String,
    @field:Schema(description = "한글 의미", example = "동맥선, 동맥 카테터")
    val meaning: String,
    @field:Schema(description = "약어. 없으면 null", example = "A-Line")
    val abbreviation: String?,
    @field:Schema(description = "상세 설명. 없으면 null", example = "지속적인 혈압 감시 및 동맥혈 채취를 위해 동맥 내에 삽입하는 카테터입니다.")
    val explanation: String?,
    @field:Schema(description = "현재 학습 중인 단어장 안에서 사용자가 북마크 했는지 여부", example = "false")
    val bookmarked: Boolean,
    @field:Schema(description = "어근/어미 분석 + 형광펜 하이라이팅 범위. 색은 클라이언트가 order 순서대로 매핑")
    val morphemes: List<OrderedMorpheme>
)

@Schema(description = "오답노트 학습 카드 1개")
data class WrongAnswerCard(
    @field:Schema(description = "단어 id", example = "293")
    val termId: Long,
    @field:Schema(description = "단어가 속한 단어장 id", example = "12")
    val glossaryId: Long,
    @field:Schema(description = "이 카드의 테스트 유형", example = "WRITE_MEANING_FROM_TERM")
    val testType: TestType,
    @field:Schema(description = "문제 텍스트, 서버가 testType 으로 derive 한 노출 문구", example = "arterial line")
    val question: String,
    @field:Schema(description = "다지선다 대표 정답 텍스트. MC 채점에 사용. 작성형은 answers 사용", example = "동맥선, 동맥 카테터")
    val answer: String,
    @field:Schema(description = "작성형 채점용 인정 정답 목록. 콤마 동의어를 분리한 결과로 하나라도 일치하면 정답. MC 카드는 answer 사용", example = "[\"동맥선\", \"동맥 카테터\"]")
    val answers: List<String>,
    @field:Schema(description = "다지선다 카드의 보기 3개. 작성형 / LISTEN_AND_WRITE 카드는 null")
    val distractors: List<String>?,
    @field:Schema(description = "영문 용어", example = "arterial line")
    val spelling: String,
    @field:Schema(description = "한글 의미", example = "동맥선, 동맥 카테터")
    val meaning: String,
    @field:Schema(description = "약어. 없으면 null", example = "A-Line")
    val abbreviation: String?,
    @field:Schema(description = "상세 설명. 없으면 null", example = "지속적인 혈압 감시 및 동맥혈 채취를 위해 동맥 내에 삽입하는 카테터입니다.")
    val explanation: String?,
    @field:Schema(description = "단어가 속한 단어장에서 사용자가 북마크 했는지 여부", example = "false")
    val bookmarked: Boolean,
    @field:Schema(description = "어근/어미 분석 + 형광펜 하이라이팅 범위")
    val morphemes: List<OrderedMorpheme>
)

@Schema(description = "커스텀노트 학습 카드 1개")
data class CustomNoteCard(
    @field:Schema(description = "단어 id", example = "293")
    val termId: Long,
    @field:Schema(description = "단어가 속한 단어장 id", example = "12")
    val glossaryId: Long,
    @field:Schema(description = "이 카드의 테스트 유형", example = "WRITE_MEANING_FROM_TERM")
    val testType: TestType,
    @field:Schema(description = "문제 텍스트, 서버가 testType 으로 derive 한 노출 문구", example = "arterial line")
    val question: String,
    @field:Schema(description = "다지선다 대표 정답 텍스트. MC 채점에 사용. 작성형은 answers 사용", example = "동맥선, 동맥 카테터")
    val answer: String,
    @field:Schema(description = "작성형 채점용 인정 정답 목록. 콤마 동의어를 분리한 결과로 하나라도 일치하면 정답. MC 카드는 answer 사용", example = "[\"동맥선\", \"동맥 카테터\"]")
    val answers: List<String>,
    @field:Schema(description = "다지선다 카드의 보기 3개. 작성형 / LISTEN_AND_WRITE 카드는 null")
    val distractors: List<String>?,
    @field:Schema(description = "영문 용어", example = "arterial line")
    val spelling: String,
    @field:Schema(description = "한글 의미", example = "동맥선, 동맥 카테터")
    val meaning: String,
    @field:Schema(description = "약어. 없으면 null", example = "A-Line")
    val abbreviation: String?,
    @field:Schema(description = "상세 설명. 없으면 null", example = "지속적인 혈압 감시 및 동맥혈 채취를 위해 동맥 내에 삽입하는 카테터입니다.")
    val explanation: String?,
    @field:Schema(description = "단어가 속한 단어장에서 사용자가 북마크 했는지 여부", example = "false")
    val bookmarked: Boolean,
    @field:Schema(description = "어근/어미 분석 + 형광펜 하이라이팅 범위")
    val morphemes: List<OrderedMorpheme>
)

@Schema(description = "과별 단어장 카드 요청")
data class GlossaryCardsRequest(
    @field:NotEmpty
    @field:Schema(
        description = "선택된 테스트 유형 리스트. 다지선다 선택 시 MC_*_FROM_* 3종을 모두 포함시켜 서버에 전송",
        example = "[\"WRITE_TERM_FROM_MEANING\", \"WRITE_ABBREVIATION_FROM_TERM\", \"MC_MEANING_FROM_TERM\", \"MC_TERM_FROM_ABBREVIATION\", \"MC_TERM_FROM_MEANING\"]"
    )
    val testTypes: List<TestType>,

    @field:NotNull
    @field:Min(1)
    @field:Max(50)
    @field:Schema(description = "출제할 카드 수 (10/20/30/40/50)", example = "30")
    val count: Int
)

@Schema(description = "커스텀노트 / 오답노트 카드 요청 (count 없음, 전체 단어 분배)")
data class NoteCardsRequest(
    @field:NotEmpty
    @field:Schema(
        description = "선택된 테스트 유형 리스트. 다지선다 선택 시 MC_*_FROM_* 3종을 모두 포함시켜 서버에 전송",
        example = "[\"WRITE_TERM_FROM_MEANING\", \"MC_MEANING_FROM_TERM\", \"MC_TERM_FROM_ABBREVIATION\", \"MC_TERM_FROM_MEANING\"]"
    )
    val testTypes: List<TestType>
)

@Schema(description = "과별 단어장 카드 응답")
data class GlossaryCardsResponse(
    @field:Schema(description = "클라이언트가 요청한 카드 수", example = "10")
    val requestedCount: Int,
    @field:Schema(description = "선택한 테스트 유형으로 출제 가능해 실제 내려간 카드 수", example = "5")
    val availableCount: Int,
    @field:Schema(description = "학습 화면에 노출할 학습 세트 단어 전체. 단어장 잔여 단어가 requestedCount 보다 적으면 그만큼만 내려감")
    val terms: List<GlossaryStudyTerm>,
    @field:Schema(description = "terms 중 선택한 테스트 유형으로 출제 가능한 단어로 만든 테스트 카드")
    val cards: List<GlossaryCard>
)

@Schema(description = "커스텀노트 카드 응답")
data class CustomNoteCardsResponse(
    val cards: List<CustomNoteCard>
)

@Schema(description = "오답노트 카드 응답")
data class WrongAnswerCardsResponse(
    val cards: List<WrongAnswerCard>
)

// 콤마로 구분된 동의어를 작성형 인정 정답 목록으로 분리
// 데이터는 V57 이후 콤마 구분자로 통일됨
fun splitAcceptableAnswers(answer: String): List<String> =
    answer.split(",").map { it.trim() }.filter { it.isNotEmpty() }

// 매핑 helper — sampling 결과 (term projection) + derive 결과 (testType/question/answer/distractors) 조합
// receiver projection 의 인터페이스 차이로 1:N 매핑 형태이므로 receiver 별 명시
fun SimpleTermProjection.toGlossaryCard(
    testType: TestType,
    question: String,
    answer: String,
    distractors: List<String>?,
    bookmarked: Boolean,
    morphemes: List<OrderedMorpheme>
): GlossaryCard = GlossaryCard(
    termId = termId,
    testType = testType,
    question = question,
    answer = answer,
    answers = splitAcceptableAnswers(answer),
    distractors = distractors,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    explanation = explanation,
    bookmarked = bookmarked,
    morphemes = morphemes
)

fun SimpleTermProjection.toStudyTerm(
    bookmarked: Boolean,
    morphemes: List<OrderedMorpheme>
): GlossaryStudyTerm = GlossaryStudyTerm(
    termId = termId,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    explanation = explanation,
    bookmarked = bookmarked,
    morphemes = morphemes
)

fun NoteCardTermProjection.toWrongAnswerCard(
    testType: TestType,
    question: String,
    answer: String,
    distractors: List<String>?,
    bookmarked: Boolean,
    morphemes: List<OrderedMorpheme>
): WrongAnswerCard = WrongAnswerCard(
    termId = termId,
    glossaryId = glossaryId,
    testType = testType,
    question = question,
    answer = answer,
    answers = splitAcceptableAnswers(answer),
    distractors = distractors,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    explanation = explanation,
    bookmarked = bookmarked,
    morphemes = morphemes
)

fun NoteCardTermProjection.toCustomNoteCard(
    testType: TestType,
    question: String,
    answer: String,
    distractors: List<String>?,
    bookmarked: Boolean,
    morphemes: List<OrderedMorpheme>
): CustomNoteCard = CustomNoteCard(
    termId = termId,
    glossaryId = glossaryId,
    testType = testType,
    question = question,
    answer = answer,
    answers = splitAcceptableAnswers(answer),
    distractors = distractors,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    explanation = explanation,
    bookmarked = bookmarked,
    morphemes = morphemes
)
