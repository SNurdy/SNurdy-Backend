package com.snurdy.backend.domain.test.glossary.dto

import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.test.common.TestType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "기본 단어장 테스트 카드 요청")
data class GlossaryTestCardsRequest(
    @field:NotEmpty
    @field:Schema(description = "학습 세트의 단어 id 목록", example = "[293, 294, 295]")
    val termIds: List<Long>,

    @field:NotEmpty
    @field:Schema(
        description = "선택된 테스트 유형 리스트. 다지선다 선택 시 MC_*_FROM_* 3종을 모두 포함",
        example = "[\"WRITE_MEANING_FROM_TERM\"]"
    )
    val testTypes: List<TestType>
)

@Schema(description = "기본 단어장 테스트 카드 응답")
data class GlossaryTestCardsResponse(
    @field:Schema(description = "선택 유형별 출제 가능 카드 수", example = "{\"WRITE_MEANING_FROM_TERM\": 10}")
    val availableCounts: Map<TestType, Int>,

    @field:Schema(description = "실제 테스트에 사용할 카드")
    val cards: List<GlossaryTestCard>
)

@Schema(description = "기본 단어장 테스트 카드 1개")
data class GlossaryTestCard(
    @field:Schema(description = "단어 id", example = "293")
    val termId: Long,
    @field:Schema(description = "이 카드의 테스트 유형", example = "WRITE_MEANING_FROM_TERM")
    val testType: TestType,
    @field:Schema(description = "문제 텍스트. LISTEN_AND_WRITE 는 클라가 화면 노출 없이 TTS 재생만", example = "arterial line")
    val question: String,
    @field:Schema(description = "다지선다 대표 정답 텍스트. MC 채점에 사용. 작성형은 answers 사용", example = "동맥선, 동맥 카테터")
    val answer: String,
    @field:Schema(description = "작성형 채점용 인정 정답 목록", example = "[\"동맥선\", \"동맥 카테터\"]")
    val answers: List<String>,
    @field:Schema(description = "다지선다 보기 3개. 작성형 / LISTEN_AND_WRITE 는 null")
    val distractors: List<String>?,
    @field:Schema(description = "영문 용어", example = "arterial line")
    val spelling: String,
    @field:Schema(description = "한글 의미", example = "동맥선, 동맥 카테터")
    val meaning: String,
    @field:Schema(description = "약어. 없으면 null", example = "A-Line")
    val abbreviation: String?,
    @field:Schema(description = "상세 설명. 없으면 null")
    val explanation: String?,
    @field:Schema(description = "북마크 여부", example = "false")
    val bookmarked: Boolean,
    @field:Schema(description = "어근/어미 분석 + 형광펜 하이라이팅 범위")
    val morphemes: List<OrderedMorpheme>
)