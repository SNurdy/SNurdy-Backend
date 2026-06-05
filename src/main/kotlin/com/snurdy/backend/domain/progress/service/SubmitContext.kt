package com.snurdy.backend.domain.progress.service

import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryTermId
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.progress.dto.LearningAnswerEvent
import com.snurdy.backend.domain.progress.entity.TermProgressEntity
import com.snurdy.backend.domain.progress.entity.TermProgressStatus
import com.snurdy.backend.domain.progress.repository.TermProgressRepository
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import java.util.UUID

// ProgressUpdateService 가 events 순회 전에 batch 로 필요한 엔티티를 prefetch 해 둔 메모리 캐시
// N+1 회피 + termId-glossary 매핑 검증 + 신규 TermProgress 생성 책임
internal class SubmitContext private constructor(
    private val validPairs: Set<GlossaryTermId>,
    private val glossaries: Map<Long, GlossaryEntity>,
    private val terms: Map<Long, TermEntity>,
    private val progresses: MutableMap<GlossaryTermId, TermProgressEntity>
) {

    fun getOrCreateProgress(
        user: UserEntity,
        event: LearningAnswerEvent,
        newProgresses: MutableList<TermProgressEntity>
    ): TermProgressEntity {
        val key = GlossaryTermId(event.glossaryId, event.termId)
        if (key !in validPairs) throw ApiException(DictionaryError.TERM_NOT_IN_GLOSSARY)

        progresses[key]?.let { return it }

        val glossary = glossaries[event.glossaryId]
            ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        val term = terms[event.termId]
            ?: throw ApiException(DictionaryError.TERM_NOT_FOUND)

        val newProgress = TermProgressEntity(
            user = user,
            glossary = glossary,
            term = term,
            status = TermProgressStatus.NEW
        )
        progresses[key] = newProgress
        newProgresses += newProgress
        return newProgress
    }

    companion object {
        fun prefetch(
            userId: UUID,
            events: List<LearningAnswerEvent>,
            glossaryTermRepository: GlossaryTermRepository,
            glossaryRepository: GlossaryRepository,
            termRepository: TermRepository,
            termProgressRepository: TermProgressRepository
        ): SubmitContext {
            val glossaryIds = events.mapTo(mutableSetOf()) { it.glossaryId }
            val termIds = events.mapTo(mutableSetOf()) { it.termId }

            val validPairs = glossaryTermRepository
                .findAllByIdGlossaryIdInAndIdTermIdIn(glossaryIds, termIds)
                .mapTo(mutableSetOf()) { it.id }

            val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id }
            val terms = termRepository.findByIdInAndDeletedAtIsNull(termIds.toList()).associateBy { it.id }
            val progresses = termProgressRepository
                .findAllByUserIdAndGlossaryIdInAndTermIdIn(userId, glossaryIds, termIds)
                .associateByTo(mutableMapOf()) { GlossaryTermId(it.glossary.id, it.term.id) }

            return SubmitContext(validPairs, glossaries, terms, progresses)
        }
    }
}
