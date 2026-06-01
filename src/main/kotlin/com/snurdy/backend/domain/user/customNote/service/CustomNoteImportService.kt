package com.snurdy.backend.domain.user.customNote.service

import com.snurdy.backend.domain.user.customNote.dto.CustomNoteResponse
import com.snurdy.backend.domain.user.customNote.dto.toResponse
import com.snurdy.backend.domain.user.customNote.entity.CustomNoteEntity
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteRepository
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteShareCodeRepository
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteTermRepository
import com.snurdy.backend.global.constants.CustomNoteError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CustomNoteImportService(
    private val customNoteRepository: CustomNoteRepository,
    private val customNoteTermRepository: CustomNoteTermRepository,
    private val customNoteShareCodeRepository: CustomNoteShareCodeRepository
) {

    // 공유 코드로 본인 단어장으로 복제
    // title 충돌 시 (2), (3) suffix 자동 증가. 단어 매핑은 batch INSERT SELECT 로 한 번에
    @Transactional
    fun importByCode(userId: UUID, code: String): CustomNoteResponse {
        val shareCode = customNoteShareCodeRepository.findByShareCodeAndRevokedAtIsNull(code)
            ?: throw ApiException(CustomNoteError.SHARE_CODE_NOT_FOUND)

        val source = customNoteRepository.findByIdOrNull(shareCode.noteId)
            ?: throw ApiException(CustomNoteError.SHARE_CODE_NOT_FOUND)

        if (customNoteRepository.countByOwnerId(userId) >= MAX_NOTES_PER_USER) {
            throw ApiException(CustomNoteError.NOTE_LIMIT_EXCEEDED)
        }

        val resolvedTitle = resolveTitle(userId, source.title)

        val newNote = try {
            customNoteRepository.save(CustomNoteEntity(ownerId = userId, title = resolvedTitle))
        } catch (_: DataIntegrityViolationException) {
            // 동시 import race - 안전망
            throw ApiException(CustomNoteError.DUPLICATED_TITLE)
        }

        val copiedCount = customNoteTermRepository.copyTermsFromNote(
            sourceNoteId = source.id,
            targetNoteId = newNote.id
        )

        return newNote.toResponse(termCount = copiedCount.toLong())
    }

    // 원본 title 그대로 시도 -> 충돌이면 " (2)", " (3)" ... 증가
    // suffix 추가로 14 codePoint 초과하면 base 부분을 잘라 길이 맞춤
    private fun resolveTitle(ownerId: UUID, originalTitle: String): String {
        if (!customNoteRepository.existsByOwnerIdAndTitle(ownerId, originalTitle)) {
            return originalTitle
        }
        for (n in 2..MAX_SUFFIX) {
            val suffix = " ($n)"
            val candidate = buildTitleWithSuffix(originalTitle, suffix)
            if (!customNoteRepository.existsByOwnerIdAndTitle(ownerId, candidate)) {
                return candidate
            }
        }
        throw ApiException(CustomNoteError.DUPLICATED_TITLE)
    }

    private fun buildTitleWithSuffix(originalTitle: String, suffix: String): String {
        val suffixCodePoints = suffix.codePointCount(0, suffix.length)
        val maxBaseCodePoints = TITLE_MAX_CODEPOINTS - suffixCodePoints
        val originalCodePoints = originalTitle.codePointCount(0, originalTitle.length)
        val base = if (originalCodePoints <= maxBaseCodePoints) {
            originalTitle
        } else {
            truncateByCodePoints(originalTitle, maxBaseCodePoints)
        }
        return "$base$suffix"
    }

    private fun truncateByCodePoints(value: String, limit: Int): String {
        if (limit <= 0) return ""
        val codePoints = value.codePoints().toArray()
        return String(codePoints, 0, minOf(limit, codePoints.size))
    }

    companion object {
        private const val MAX_NOTES_PER_USER = 20L
        private const val MAX_SUFFIX = 100
        private const val TITLE_MAX_CODEPOINTS = 14
    }
}
