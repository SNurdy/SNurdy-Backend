package com.snurdy.backend.domain.dictionary.morpheme.service

import com.snurdy.backend.domain.dictionary.dto.MorphemeHighlight
import com.snurdy.backend.domain.dictionary.dto.OrderedMorpheme
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantProjection
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeVariantRepository
import com.snurdy.backend.domain.dictionary.term.repository.BulkOrderedMorphemeProjection
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import org.springframework.stereotype.Component

// 단어 ids + spelling 을 받아 카드 응답의 morphemes 필드 빌드
// 어근/어미 row + variant row 를 모아 highlight 범위까지 계산
@Component
class MorphemeHighlightAssembler(
    private val termMorphemeRepository: TermMorphemeRepository,
    private val morphemeVariantRepository: MorphemeVariantRepository,
    private val highlightCalculator: HighlightCalculator
) {

    fun buildByTermIds(
        termIds: List<Long>,
        spellingByTermId: Map<Long, String>
    ): Map<Long, List<OrderedMorpheme>> {
        if (termIds.isEmpty()) return emptyMap()
        val morphemesByTermId = termMorphemeRepository
            .findOrderedMorphemesByTermIds(termIds)
            .groupBy { it.termId }
        val variantsByMorphemeId = fetchVariantsByMorphemeId(morphemesByTermId.values.flatten())

        return termIds.associateWith { termId ->
            val spelling = spellingByTermId[termId] ?: return@associateWith emptyList()
            buildOrderedMorphemes(spelling, morphemesByTermId[termId].orEmpty(), variantsByMorphemeId)
        }
    }

    private fun fetchVariantsByMorphemeId(
        morphemes: Collection<BulkOrderedMorphemeProjection>
    ): Map<Long, List<MorphemeVariantProjection>> {
        val morphemeIds = morphemes.map { it.morphemeId }.distinct()
        if (morphemeIds.isEmpty()) return emptyMap()
        return morphemeVariantRepository
            .findByMorphemeIds(morphemeIds)
            .groupBy { it.morphemeId }
    }

    private fun buildOrderedMorphemes(
        spelling: String,
        morphemes: List<BulkOrderedMorphemeProjection>,
        variantsByMorphemeId: Map<Long, List<MorphemeVariantProjection>>
    ): List<OrderedMorpheme> = morphemes.map { morpheme ->
        val variants = variantsByMorphemeId[morpheme.morphemeId].orEmpty()
        val range = highlightCalculator.calculate(spelling, variants)
        OrderedMorpheme(
            id = morpheme.morphemeId,
            spelling = morpheme.spelling,
            meaning = morpheme.meaning,
            order = morpheme.position,
            highlight = range?.let { MorphemeHighlight(start = it.start, length = it.length) }
        )
    }
}
