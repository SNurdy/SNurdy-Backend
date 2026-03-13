package com.snurdy.backend.domain.migration.service

import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.catalog.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.catalog.term.repository.TermRepository
import com.snurdy.backend.domain.learning.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.learning.repository.TermProgressRepository
import com.snurdy.backend.domain.learning.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.learning.entity.GlossaryProgressId
import com.snurdy.backend.domain.learning.entity.TermProgressEntity
import com.snurdy.backend.domain.learning.entity.TermProgressStatus
import com.snurdy.backend.domain.migration.dto.GlossaryProgressPayload
import com.snurdy.backend.domain.migration.dto.MigrationStartResponse
import com.snurdy.backend.domain.migration.dto.MigrationStatusResponse
import com.snurdy.backend.domain.migration.dto.MigrationUploadRequest
import com.snurdy.backend.domain.migration.dto.MigrationUploadResponse
import com.snurdy.backend.domain.migration.dto.TermProgressPayload
import com.snurdy.backend.domain.migration.entity.MigrationSessionEntity
import com.snurdy.backend.domain.migration.entity.MigrationStatus
import com.snurdy.backend.domain.migration.repository.MigrationSessionRepository
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.constants.MigrationError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MigrationService(
    private val migrationSessionRepository: MigrationSessionRepository,
    private val termProgressRepository: TermProgressRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val userRepository: UserRepository,
    private val termRepository: TermRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val glossaryRepository: GlossaryRepository
) {

    @Transactional
    fun start(userId: UUID): MigrationStartResponse {
        val user = findUser(userId)

        val existing = migrationSessionRepository.findByUserId(userId)
        if (existing != null) {
            if (existing.isCompleted()) throw ApiException(MigrationError.MIGRATION_ALREADY_COMPLETED)
            return MigrationStartResponse(migrationId = existing.id.toString())
        }

        return try {
            val session = migrationSessionRepository.save(MigrationSessionEntity(user = user))
            MigrationStartResponse(migrationId = session.id.toString())
        } catch (_: DataIntegrityViolationException) {
            val conflicted = migrationSessionRepository.findByUserId(userId)
                ?: throw ApiException(CommonError.INTERNAL_ERROR)
            if (conflicted.isCompleted()) throw ApiException(MigrationError.MIGRATION_ALREADY_COMPLETED)
            MigrationStartResponse(migrationId = conflicted.id.toString())
        }
    }

    @Transactional
    fun upload(userId: UUID, migrationId: String, request: MigrationUploadRequest): MigrationUploadResponse {
        validateDuplicateEntries(request)

        val session = findSessionForUpdate(migrationId)
        if (session.user.id != userId) throw ApiException(CommonError.FORBIDDEN)
        if (session.isCompleted()) throw ApiException(MigrationError.MIGRATION_ALREADY_COMPLETED)

        if (session.isUploaded() || session.isCompleted()) {
            return MigrationUploadResponse(
                migrationId = migrationId,
                termProgressCount = termProgressRepository.countByUserId(userId).toInt(),
                glossaryProgressCount = glossaryProgressRepository.countByUserId(userId).toInt()
            )
        }

        if (termProgressRepository.existsByUserId(userId) ||
            glossaryProgressRepository.existsByUserId(userId)) {
            throw ApiException(MigrationError.MIGRATION_DATA_CONFLICT)
        }

        val user = findUser(userId)
        val savedTermCount = saveTermProgresses(user, request.termProgresses)
        val savedGlossaryCount = saveGlossaryProgresses(user, request.glossaryProgresses)

        session.markUploaded()
        migrationSessionRepository.save(session)

        return MigrationUploadResponse(
            migrationId = migrationId,
            termProgressCount = savedTermCount,
            glossaryProgressCount = savedGlossaryCount
        )
    }

    @Transactional
    fun finalize(userId: UUID, migrationId: String): MigrationStatusResponse {
        val session = findSessionForUpdate(migrationId)
        if (session.user.id != userId) throw ApiException(CommonError.FORBIDDEN)
        if (session.isCompleted()) throw ApiException(MigrationError.MIGRATION_ALREADY_COMPLETED)

        if (!session.isUploaded()) throw ApiException(MigrationError.MIGRATION_UPLOAD_REQUIRED)

        session.complete()
        migrationSessionRepository.save(session)

        return MigrationStatusResponse(
            status = MigrationStatus.COMPLETED.name,
            completedAt = session.completedAt
        )
    }

    @Transactional(readOnly = true)
    fun getStatus(userId: UUID): MigrationStatusResponse {
        val session = migrationSessionRepository.findByUserId(userId)
            ?: return MigrationStatusResponse(status = MigrationStatus.NOT_STARTED.name)

        return MigrationStatusResponse(
            status = session.status.name,
            completedAt = session.completedAt,
            errorMessage = session.errorMessage
        )
    }

    private fun saveTermProgresses(user: UserEntity, payloads: List<TermProgressPayload>): Int {
        val glossaryIds = payloads.map { it.glossaryId }.distinct()
        val termIds = payloads.map { it.termId }.distinct()

        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id!! }
        val terms = termRepository.findByIdInAndDeletedAtIsNull(termIds).associateBy { it.id!! }

        val missingGlossaryIds = glossaryIds.filterNot(glossaries::containsKey)
        if (missingGlossaryIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_ID)
        }

        val missingTermIds = termIds.filterNot(terms::containsKey)
        if (missingTermIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_TERM_ID)
        }

        val validPairs = glossaryTermRepository
            .findGlossariesByTermIds(termIds)
            .map { it.termId to it.glossaryId }
            .toSet()

        val invalidPairs = payloads
            .filter { (it.termId to it.glossaryId) !in validPairs }
            .map { it.termId to it.glossaryId }

        if (invalidPairs.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_TERM_PAIR)
        }

        val entities = payloads.map { payload ->
            val glossary = glossaries[payload.glossaryId]!!
            val term = terms[payload.termId]!!

            TermProgressEntity(
                user = user,
                glossary = glossary,
                term = term,
                status = parseTermProgressStatus(payload.status),
                easeFactor = payload.easeFactor.toBigDecimal(),
                intervalDays = payload.intervalDays,
                reviewCount = payload.reviewCount,
                lastReviewedAt = payload.lastReviewedAt,
                nextReviewAt = payload.nextReviewAt,
                clientGeneratedId = payload.clientGeneratedId,
                occurredAt = payload.occurredAt
            )
        }

        termProgressRepository.saveAll(entities)
        return entities.size
    }

    private fun saveGlossaryProgresses(user: UserEntity, payloads: List<GlossaryProgressPayload>): Int {
        val glossaryIds = payloads.map { it.glossaryId }.distinct()
        val glossaries = glossaryRepository.findAllById(glossaryIds).associateBy { it.id!! }

        val missingGlossaryIds = glossaryIds.filterNot(glossaries::containsKey)
        if (missingGlossaryIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_INVALID_GLOSSARY_ID)
        }

        val entities = payloads.map { payload ->
            val glossary = glossaries[payload.glossaryId]!!

            GlossaryProgressEntity(
                id = GlossaryProgressId(userId = user.id, glossaryId = payload.glossaryId),
                user = user,
                glossary = glossary,
                learnedCount = payload.learnedCount,
                lastLearnedAt = payload.lastLearnedAt
            )
        }

        glossaryProgressRepository.saveAll(entities)
        return entities.size
    }

    private fun validateDuplicateEntries(request: MigrationUploadRequest) {
        val duplicatedTermKeys = request.termProgresses
            .groupingBy { it.glossaryId to it.termId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicatedTermKeys.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_DUPLICATED_TERM_PROGRESS)
        }

        val duplicatedGlossaryIds = request.glossaryProgresses
            .groupingBy { it.glossaryId }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicatedGlossaryIds.isNotEmpty()) {
            throw ApiException(MigrationError.MIGRATION_DUPLICATED_GLOSSARY_PROGRESS)
        }
    }

    private fun findUser(userId: UUID): UserEntity =
        userRepository.findByIdOrNull(userId) ?: throw ApiException(CommonError.NOT_FOUND)

    private fun findSession(migrationId: String): MigrationSessionEntity {
        val uuid = runCatching { UUID.fromString(migrationId) }
            .getOrElse { throw ApiException(MigrationError.MIGRATION_SESSION_NOT_FOUND) }
        return migrationSessionRepository.findByIdOrNull(uuid)
            ?: throw ApiException(MigrationError.MIGRATION_SESSION_NOT_FOUND)
    }

    private fun findSessionForUpdate(migrationId: String): MigrationSessionEntity {
        val uuid = runCatching { UUID.fromString(migrationId) }
            .getOrElse { throw ApiException(MigrationError.MIGRATION_SESSION_NOT_FOUND) }
        return migrationSessionRepository.findByIdForUpdate(uuid)
            ?: throw ApiException(MigrationError.MIGRATION_SESSION_NOT_FOUND)
    }

    private fun parseTermProgressStatus(status: String): TermProgressStatus =
        runCatching { TermProgressStatus.valueOf(status) }
            .getOrElse { throw ApiException(MigrationError.MIGRATION_INVALID_TERM_STATUS) }
}