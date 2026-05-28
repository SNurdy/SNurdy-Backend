package com.snurdy.backend.domain.dictionary.dto

import com.snurdy.backend.domain.dictionary.glossary.repository.BulkTermGlossaryProjection
import com.snurdy.backend.domain.dictionary.glossary.repository.ClientGlossaryTermProjection
import com.snurdy.backend.domain.dictionary.glossary.repository.TermGlossaryProjection
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import io.swagger.v3.oas.annotations.media.Schema

// 단어 자체/단어장 태그/형태소 등 사전 데이터 공용 표현.
// 단어 상세 응답과 어근/어미 검색 응답 모두 재사용한다.

@Schema(description = "용어 상세")
data class TermDetail(
    @field:Schema(description = "용어 ID", example = "618")
    val id: Long,
    @field:Schema(description = "철자", example = "Hepatitis")
    val spelling: String,
    @field:Schema(description = "뜻", example = "간염")
    val meaning: String,
    @field:Schema(description = "약어", example = "")
    val abbreviation: String?,
    @field:Schema(description = "설명")
    val explanation: String?
)

@Schema(description = "용어 상세 내 용어집 태그")
data class TermGlossaryTag(
    @field:Schema(description = "용어집 ID", example = "6")
    val id: Long,
    @field:Schema(description = "용어집 카테고리", example = "내과")
    val category: String,
    @field:Schema(description = "용어집 제목", example = "심장내과")
    val title: String
)

@Schema(description = "순서 포함 어근어미")
data class OrderedMorpheme(
    @field:Schema(description = "어근어미 ID", example = "361")
    val id: Long,
    @field:Schema(description = "어근어미 철자", example = "hepat(o)-")
    val spelling: String,
    @field:Schema(description = "어근어미 의미", example = "간")
    val meaning: String,
    @field:Schema(description = "용어 내 순서", example = "0")
    val order: Int,
    @field:Schema(description = "하이라이팅 범위 (null이면 매칭 실패)")
    val highlight: MorphemeHighlight? = null
)

@Schema(description = "어근어미 하이라이팅 범위")
data class MorphemeHighlight(
    @field:Schema(description = "하이라이팅 시작 인덱스 (0-based)", example = "0")
    val start: Int,
    @field:Schema(description = "하이라이팅 길이", example = "6")
    val length: Int
)

// 사전 검색/조회/단어장 응답. 모두 사용자 컨텍스트 (북마크/메모/진행률) 결합.

@Schema(description = "용어 검색 응답")
data class TermSearchResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val terms: List<TermSummary>
)

@Schema(description = "용어 요약 (검색 결과 카드용, 본인 북마크 포함)")
data class TermSummary(
    val id: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val bookmarked: Boolean
)

@Schema(description = "용어 상세 응답 (본인 북마크/메모 포함)")
data class TermDetailResponse(
    val term: TermDetail,
    val glossaries: List<TermGlossaryTag>,
    val morphemes: List<OrderedMorpheme>,
    val bookmarked: Boolean,
    val bookmarkedGlossaryIds: List<Long>,
    @field:Schema(description = "본인이 이 단어에 작성한 메모. 없으면 null", example = "혈전 관련 검사 항목")
    val memo: String?
)

@Schema(description = "용어장 목록 응답")
data class GlossaryListResponse(
    val glossaries: List<GlossarySummary>
)

@Schema(description = "용어장 요약 (본인 학습 진행률/레벨 포함)")
data class GlossarySummary(
    val id: Long,
    val category: String,
    val title: String,
    val termCount: Long,
    val learnedCount: Int,
    val progressRate: Double,
    val level: Int
)

@Schema(description = "용어장 내 용어 목록 응답")
data class GlossaryTermsResponse(
    val glossaryId: Long,
    val category: String,
    val title: String,
    val filter: String,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val terms: List<GlossaryTermItem>
)

@Schema(description = "용어장 내 용어 항목 (본인 학습 상태/북마크 포함)")
data class GlossaryTermItem(
    val id: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val status: String,
    val bookmarked: Boolean
)

// TermEntity 가 두 응답 (TermSummary / TermDetail) 로 변환되어 1:N 케이스 - 의미 키워드로 구분
fun TermEntity.toSummary(bookmarked: Boolean) = TermSummary(
    id = id,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    bookmarked = bookmarked
)

fun TermEntity.toDetail() = TermDetail(
    id = id,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    explanation = explanation
)

fun TermGlossaryProjection.toResponse() = TermGlossaryTag(
    id = glossaryId,
    category = category,
    title = title
)

fun BulkTermGlossaryProjection.toResponse() = TermGlossaryTag(
    id = glossaryId,
    category = category,
    title = title
)

fun ClientGlossaryTermProjection.toResponse() = GlossaryTermItem(
    id = termId,
    spelling = spelling,
    meaning = meaning,
    abbreviation = abbreviation,
    status = status,
    bookmarked = bookmarked == 1
)
