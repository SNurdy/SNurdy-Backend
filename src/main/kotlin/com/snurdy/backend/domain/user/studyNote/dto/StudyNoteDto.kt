package com.snurdy.backend.domain.user.studyNote.dto

import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.dictionary.dto.TermDetail
import com.snurdy.backend.domain.dictionary.dto.TermGlossaryTag
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "학습 노트 단어 검색 응답")
data class StudyNoteTermSearchResponse(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val terms: List<StudyNoteTermSummary>
)

@Schema(description = "학습 노트 단어 요약 (북마크 포함)")
data class StudyNoteTermSummary(
    val id: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val bookmarked: Boolean
)

@Schema(description = "학습 노트 단어 상세 (북마크 포함)")
data class StudyNoteTermDetailResponse(
    val term: TermDetail,
    val glossaries: List<TermGlossaryTag>,
    val morphemes: List<OrderedMorpheme>,
    val bookmarked: Boolean,
    val bookmarkedGlossaryIds: List<Long>
)

@Schema(description = "학습 노트 목록 응답")
data class StudyNoteListResponse(
    val notes: List<StudyNoteSummary>
)

@Schema(description = "학습 노트 요약 (진행률 포함)")
data class StudyNoteSummary(
    val id: Long,
    val category: String,
    val title: String,
    val termCount: Long,
    val learnedCount: Int,
    val progressRate: Double
)

@Schema(description = "학습 노트 내 단어 목록 응답")
data class StudyNoteTermsResponse(
    val glossaryId: Long,
    val category: String,
    val title: String,
    val filter: String,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val terms: List<StudyNoteTermItem>
)

@Schema(description = "학습 노트 내 단어 항목")
data class StudyNoteTermItem(
    val id: Long,
    val spelling: String,
    val meaning: String,
    val abbreviation: String?,
    val status: String,
    val bookmarked: Boolean
)
