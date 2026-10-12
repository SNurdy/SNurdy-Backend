package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.SimpleTermProjection
import com.snurdy.backend.domain.dictionary.morpheme.service.MorphemeHighlightAssembler
import com.snurdy.backend.domain.test.common.dto.TestType
import com.snurdy.backend.domain.test.common.dto.deriveAnswer
import com.snurdy.backend.domain.test.common.dto.isMultipleChoice
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestCard
import com.snurdy.backend.domain.test.glossary.dto.toTestCard
import org.springframework.stereotype.Component
import java.util.UUID

// 출제할 단어와 유형이 정해진 뒤 화면에 보여줄 카드 정보를 여러 도메인에서 모아 채운다
@Component
class GlossaryTestCardAssembler(
    private val glossaryTermRepository: GlossaryTermRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val distractorSelector: GlossaryDistractorSelector,
    private val morphemeHighlightAssembler: MorphemeHighlightAssembler
) {

    // 카드마다 따로 조회하지 않도록 필요한 정보를 한 번에 모은 뒤 카드로 조립한다
    fun assembleCards(
        userId: UUID,
        glossaryId: Long,
        assignments: List<Pair<SimpleTermProjection, TestType>>
    ): List<GlossaryTestCard> {
        val terms = assignments.map { it.first }.distinctBy { it.termId }
        val distractorPool = findDistractorPool(glossaryId, assignments)
        val bookmarkedTermIds = findBookmarkedTermIds(userId, glossaryId, terms)
        val morphemesByTermId = buildMorphemesByTermId(terms)

        return assignments.map { (term, testType) ->
            val answer = testType.deriveAnswer(term.spelling, term.meaning, term.abbreviation)
            term.toTestCard(
                testType = testType,
                answer = answer,
                distractors = selectDistractors(term, testType, answer, distractorPool),
                bookmarked = term.termId in bookmarkedTermIds,
                morphemes = morphemesByTermId[term.termId].orEmpty()
            )
        }
    }

    // 객관식 오답 보기를 같은 단어장 단어에서 고르기 위한 후보 목록이며 객관식이 없으면 조회를 생략한다
    private fun findDistractorPool(
        glossaryId: Long,
        assignments: List<Pair<SimpleTermProjection, TestType>>
    ): List<SimpleTermProjection> =
        if (assignments.any { it.second.isMultipleChoice() }) {
            glossaryTermRepository.findAllSimpleTermsByGlossaryId(glossaryId)
        } else {
            emptyList()
        }

    // 카드에 사용자 북마크 여부를 표시하기 위해 출제 단어 중 북마크한 단어만 한 번에 조회한다
    private fun findBookmarkedTermIds(
        userId: UUID,
        glossaryId: Long,
        terms: List<SimpleTermProjection>
    ): Set<Long> =
        bookmarkRepository
            .findBookmarkedTermIds(userId, glossaryId, terms.map { it.termId })
            .toSet()

    // 카드에 어근 어미 하이라이트를 보여주기 위해 출제 단어의 형태소 정보를 한 번에 만든다
    private fun buildMorphemesByTermId(terms: List<SimpleTermProjection>): Map<Long, List<OrderedMorpheme>> =
        morphemeHighlightAssembler.buildByTermIds(
            termIds = terms.map { it.termId },
            spellingByTermId = terms.associate { it.termId to it.spelling }
        )

    // 오답 보기는 객관식 카드에만 필요하고 작성형 카드는 null 로 내려 앱이 보기 영역을 그리지 않게 한다
    private fun selectDistractors(
        term: SimpleTermProjection,
        testType: TestType,
        answer: String,
        distractorPool: List<SimpleTermProjection>
    ): List<String>? =
        if (testType.isMultipleChoice()) {
            distractorSelector.select(term.termId, answer, testType, distractorPool)
        } else {
            null
        }
}