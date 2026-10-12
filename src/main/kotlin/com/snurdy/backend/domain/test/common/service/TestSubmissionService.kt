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

    // 사용자 행 잠금 이후에 호출해야 동시에 온 같은 키 요청이 선행 요청의 기록을 본다
    @Transactional(propagation = Propagation.MANDATORY)
    fun <T> findPreviousResponse(
        userId: UUID,
        idempotencyKey: UUID,
        responseType: Class<T>
    ): T? {
        val previous = testSubmissionRepository
            .findByIdOrNull(TestSubmissionId(userId, idempotencyKey))
            ?: return null
        return objectMapper.readValue(previous.responseBody, responseType)
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun saveResponse(
        userId: UUID,
        idempotencyKey: UUID,
        submissionType: TestSubmissionType,
        response: Any
    ) {
        testSubmissionRepository.save(
            TestSubmissionEntity(
                id = TestSubmissionId(userId, idempotencyKey),
                submissionType = submissionType,
                responseBody = objectMapper.writeValueAsString(response)
            )
        )
    }
}