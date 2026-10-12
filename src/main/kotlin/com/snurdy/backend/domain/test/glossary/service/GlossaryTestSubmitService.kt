package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.progress.service.ProgressUpdateService
import com.snurdy.backend.domain.test.common.entity.TestSubmissionType
import com.snurdy.backend.domain.test.common.service.TestSubmissionService
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitRequest
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestSubmitResponse
import com.snurdy.backend.domain.test.glossary.dto.toSubmitResponse
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TestError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.snurdy.backend.domain.test.glossary.dto.toProgressEvent
import java.util.UUID

@Service
class GlossaryTestSubmitService(
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val progressUpdateService: ProgressUpdateService,
    private val testSubmissionService: TestSubmissionService,
    private val snapshotReader: GlossaryTestSnapshotReader
) {

    @Transactional
    fun submitTest(
        userId: UUID,
        glossaryId: Long,
        request: GlossaryTestSubmitRequest,
        idempotencyKey: UUID?
    ): GlossaryTestSubmitResponse {
        progressUpdateService.lockUserForProgressUpdate(userId)

        testSubmissionService
            .findPreviousResponse(userId, idempotencyKey, GlossaryTestSubmitResponse::class.java)
            ?.let { return it }

        requireGlossaryExists(glossaryId)
        val results = request.results.distinctBy { it.termId }
        validateTermIdsBelongToGlossary(glossaryId, results.map { it.termId })

        val before = snapshotReader.readSnapshot(userId, glossaryId)
        progressUpdateService.updateProgress(userId, results.map { it.toProgressEvent(glossaryId) })
        val after = snapshotReader.readSnapshot(userId, glossaryId)

        val response = results.toSubmitResponse(glossaryId, request.studiedTermCount, before, after)
        testSubmissionService.saveResponse(userId, idempotencyKey, TestSubmissionType.GLOSSARY, response)
        return response
    }

    private fun requireGlossaryExists(glossaryId: Long) {
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }
    }

    private fun validateTermIdsBelongToGlossary(glossaryId: Long, termIds: List<Long>) {
        val requested = termIds.toSet()
        val matched = glossaryTermRepository
            .findAllByIdGlossaryIdAndIdTermIdIn(glossaryId, requested)
            .mapTo(mutableSetOf()) { it.id.termId }
        if (matched.size != requested.size) {
            throw ApiException(TestError.TEST_RESULT_INVALID)
        }
    }
}