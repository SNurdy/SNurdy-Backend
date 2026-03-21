package com.snurdy.backend.domain.migration.service

import com.snurdy.backend.domain.migration.repository.MigrationSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MigrationSessionFailureService(
    private val migrationSessionRepository: MigrationSessionRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markFailed(sessionId: UUID, reason: String) {
        val session = migrationSessionRepository.findById(sessionId).orElse(null) ?: return
        if (session.isCompleted()) return
        session.fail(reason)
        migrationSessionRepository.save(session)
    }
}
