// Responsibility: 용어(Term)와 형태소·용어집 간 매핑 관계의 교체(delete+insert)
package com.snurdy.backend.domain.admin.workflow.service.support

import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermEntity
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermId
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.morpheme.repository.MorphemeRepository
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.dictionary.term.entity.TermMorphemeEntity
import com.snurdy.backend.domain.dictionary.term.entity.TermMorphemeId
import com.snurdy.backend.domain.dictionary.term.repository.TermMorphemeRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.global.constants.AdminError
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component

@Component
class TermRelationManager(
    private val termRepository: TermRepository,
    private val morphemeRepository: MorphemeRepository,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termMorphemeRepository: TermMorphemeRepository
) {

    fun replaceMorphemeMappings(term: TermEntity, morphemeIds: List<Long>) {
        val termId = term.id ?: throw ApiException(DictionaryError.TERM_NOT_FOUND)
        termMorphemeRepository.deleteByIdTermId(termId)
        if (morphemeIds.isEmpty()) return

        val morphemeById = morphemeRepository.findAllById(morphemeIds)
            .mapNotNull { m -> m.id?.let { it to m } }
            .toMap()
        val mappings = morphemeIds.mapIndexed { index, morphemeId ->
            val morpheme = morphemeById[morphemeId] ?: throw ApiException(AdminError.MORPHEME_NOT_FOUND)
            TermMorphemeEntity(id = TermMorphemeId(termId = termId, position = index), term = term, morpheme = morpheme)
        }
        termMorphemeRepository.saveAll(mappings)
    }

    fun replaceGlossaryMappings(termId: Long, glossaryIds: List<Long>) {
        val normalizedIds = glossaryIds.distinct()
        val glossaryById = glossaryRepository.findAllById(normalizedIds)
            .mapNotNull { g -> g.id?.let { it to g } }
            .toMap()
        if (glossaryById.size != normalizedIds.size) throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)

        val term = termRepository.findById(termId).orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }
        glossaryTermRepository.deleteByIdTermId(termId)
        if (normalizedIds.isEmpty()) return

        val mappings = normalizedIds.map { glossaryId ->
            val glossary = glossaryById[glossaryId] ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
            GlossaryTermEntity(
                id = GlossaryTermId(glossaryId = glossaryId, termId = termId),
                glossary = glossary,
                term = term
            )
        }
        glossaryTermRepository.saveAll(mappings)
    }
}
