package com.snurdy.backend.domain.dictionary.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.dto.ClientTermDetailResponse
import com.snurdy.backend.domain.dictionary.dto.MorphemeHighlight
import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.dictionary.dto.TermDetail
import com.snurdy.backend.domain.dictionary.dto.TermDetailResponse
import com.snurdy.backend.domain.dictionary.dto.TermGlossaryTag
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.domain.dictionary.morpheme.service.HighlightCalculator
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.user.termMemo.repository.UserTermMemoRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// 단어 상세 응답 책임 전담
// 사전 raw 데이터 (term + glossaries + morphemes + highlight) 를 조립하고
// 본인 북마크/메모 컨텍스트를 결합해서 BFF 응답을 만든다
@Service
class TermDetailService(
    private val termRepository: TermRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termMorphemeRepository: TermMorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val highlightCalculator: HighlightCalculator,
    private val bookmarkRepository: BookmarkRepository,
    private val userTermMemoRepository: UserTermMemoRepository
) {

    @Transactional(readOnly = true)
    fun getTermDetail(userId: UUID, termId: Long): ClientTermDetailResponse {
        val raw = assembleRawDetail(termId)
        val bookmarks = bookmarkRepository.findAllByIdUserIdAndIdTermId(userId, termId)
        val memo = userTermMemoRepository.findByIdUserIdAndIdTermId(userId, termId)

        return ClientTermDetailResponse(
            term = raw.term,
            glossaries = raw.glossaries,
            morphemes = raw.morphemes,
            bookmarked = bookmarks.isNotEmpty(),
            bookmarkedGlossaryIds = bookmarks.map { it.id.glossaryId }.distinct().sorted(),
            memo = memo?.content
        )
    }

    // 단어 상세의 사전 raw 부분 (사용자 무관) 조립.
    // 관리자 도메인에서도 같은 raw 가 필요할 경우 재사용할 수 있도록 별도 메서드로 노출.
    fun assembleRawDetail(termId: Long): TermDetailResponse {
        val term = termRepository.findByIdAndDeletedAtIsNull(termId)
            .orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }

        val glossaries = glossaryTermRepository.findGlossariesByTermId(termId)
            .map {
                TermGlossaryTag(
                    id = it.glossaryId,
                    category = it.category,
                    title = it.title
                )
            }

        val morphemes = termMorphemeRepository.findOrderedMorphemesByTermId(termId)
        val morphemeIds = morphemes.map { it.morphemeId }
        val variantsByMorphemeId = if (morphemeIds.isNotEmpty()) {
            morphemeVariantRepository.findByMorphemeIds(morphemeIds)
                .groupBy { it.morphemeId }
        } else {
            emptyMap()
        }

        val orderedMorphemes = morphemes.map { m ->
            val variants = variantsByMorphemeId[m.morphemeId] ?: emptyList()
            val range = highlightCalculator.calculate(term.spelling, variants)
            OrderedMorpheme(
                id = m.morphemeId,
                spelling = m.spelling,
                meaning = m.meaning,
                order = m.position,
                highlight = range?.let { MorphemeHighlight(start = it.start, length = it.length) }
            )
        }

        return TermDetailResponse(
            term = TermDetail(
                id = term.id,
                spelling = term.spelling,
                meaning = term.meaning,
                abbreviation = term.abbreviation,
                explanation = term.explanation
            ),
            glossaries = glossaries,
            morphemes = orderedMorphemes
        )
    }
}
