package com.snurdy.backend.domain.learning.service

import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.learning.dto.LearningTermCard
import com.snurdy.backend.domain.learning.dto.ReviewDueTermsResponse
import com.snurdy.backend.domain.learning.dto.StudyNextTermsResponse
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class LearningQueryService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val termRepository: TermRepository,
    private val termProgressRepository: TermProgressRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun getNextTerms(userId: UUID, glossaryId: Long, size: Int): StudyNextTermsResponse {
        val glossary = findGlossary(glossaryId)

        val cards = glossaryTermRepository
            .findNewLearningCards(userId, glossary.id, size)
            .map { LearningTermCard.fromNewProjection(it) }

        return StudyNextTermsResponse(glossaryId = glossary.id, terms = cards)
    }

    @Transactional(readOnly = true)
    fun getDueTerms(userId: UUID, glossaryId: Long, size: Int): ReviewDueTermsResponse {
        val glossary = findGlossary(glossaryId)
        val now = clock.instant()

        val dueProgresses = termProgressRepository.findDueProgresses(
            userId = userId,
            glossaryId = glossary.id,
            now = now,
            statuses = listOf(TermProgressStatus.LEARNING, TermProgressStatus.REVIEW),
            pageable = PageRequest.of(0, size)
        )

        val termIds = dueProgresses.map { it.term.id }
        val termMap = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id }
        val bookmarkedTermIds = if (termIds.isEmpty()) {
            emptySet()
        } else {
            bookmarkRepository.findAllByIdUserIdAndIdTermIdIn(userId, termIds)
                .map { it.id.termId }
                .toSet()
        }

        val cards = dueProgresses.mapNotNull { progress ->
            val term = termMap[progress.term.id] ?: return@mapNotNull null
            LearningTermCard.from(progress, term, term.id in bookmarkedTermIds)
        }

        return ReviewDueTermsResponse(glossaryId = glossary.id, terms = cards)
    }

    private fun findGlossary(glossaryId: Long): GlossaryEntity =
        glossaryRepository.findByIdOrNull(glossaryId)
            ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
}
