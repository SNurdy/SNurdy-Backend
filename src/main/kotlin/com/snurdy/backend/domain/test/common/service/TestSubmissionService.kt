package com.snurdy.backend.domain.test.common.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.snurdy.backend.domain.test.common.entity.TestSubmissionEntity
import com.snurdy.backend.domain.test.common.entity.TestSubmissionId
import com.snurdy.backend.domain.test.common.entity.TestSubmissionType
import com.snurdy.backend.domain.test.common.repository.TestSubmissionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TestSubmissionService(
    private val testSubmissionRepository: TestSubmissionRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional(propagation = Propagation.MANDATORY)
    fun <T> findPreviousResponse(
        userId: UUID,
        idempotencyKey: UUID?,
        responseType: Class<T>
    ): T? {
        if (idempotencyKey == null) return null
        val previous = testSubmissionRepository
            .findByIdOrNull(TestSubmissionId(userId, idempotencyKey))
            ?: return null
        return objectMapper.readValue(previous.responseBody, responseType)
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun saveResponse(
        userId: UUID,
        idempotencyKey: UUID?,
        submissionType: TestSubmissionType,
        response: Any
    ) {
        if (idempotencyKey == null) return
        testSubmissionRepository.save(
            TestSubmissionEntity(
                id = TestSubmissionId(userId, idempotencyKey),
                submissionType = submissionType,
                responseBody = objectMapper.writeValueAsString(response)
            )
        )
    }
}