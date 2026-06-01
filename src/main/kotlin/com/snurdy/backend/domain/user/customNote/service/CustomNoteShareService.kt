package com.snurdy.backend.domain.user.customNote.service

import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.domain.user.customNote.config.SharingProperties
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteShareIssueResponse
import com.snurdy.backend.domain.user.customNote.entity.CustomNoteEntity
import com.snurdy.backend.domain.user.customNote.entity.CustomNoteShareCodeEntity
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteRepository
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteShareCodeRepository
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteTermRepository
import com.snurdy.backend.global.constants.CustomNoteError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class CustomNoteShareService(
    private val customNoteRepository: CustomNoteRepository,
    private val customNoteTermRepository: CustomNoteTermRepository,
    private val customNoteShareCodeRepository: CustomNoteShareCodeRepository,
    private val userRepository: UserRepository,
    private val shareCodeGenerator: ShareCodeGenerator,
    private val sharingProperties: SharingProperties,
    private val clock: Clock
) {

    // 본인 단어장의 공유 코드를 발급 (또는 활성 코드 그대로 반환)
    // revoked 상태였다면 같은 row 에 새 코드 덮어쓰기 (회전)
    @Transactional
    fun issue(userId: UUID, noteId: Long): CustomNoteShareIssueResponse {
        val note = findOwned(userId, noteId)
        val now = clock.instant()

        val existing = customNoteShareCodeRepository.findByIdOrNull(noteId)
        val saved = when {
            existing == null -> {
                val newCode = shareCodeGenerator.generateUnique()
                customNoteShareCodeRepository.save(
                    CustomNoteShareCodeEntity(noteId = noteId, shareCode = newCode, issuedAt = now)
                )
            }
            existing.isActive() -> existing
            else -> {
                existing.reissue(newCode = shareCodeGenerator.generateUnique(), at = now)
                existing
            }
        }

        return buildIssueResponse(note, saved.shareCode)
    }

    // 공유 비활성화. row 가 없거나 이미 비활성이면 idempotent 처리 (에러 없음)
    @Transactional
    fun revoke(userId: UUID, noteId: Long) {
        findOwned(userId, noteId)
        val existing = customNoteShareCodeRepository.findByIdOrNull(noteId) ?: return
        if (existing.isActive()) {
            existing.revoke(clock.instant())
        }
    }

    private fun findOwned(userId: UUID, noteId: Long): CustomNoteEntity {
        val note = customNoteRepository.findByIdOrNull(noteId)
            ?: throw ApiException(CustomNoteError.NOT_FOUND)
        if (note.ownerId != userId) {
            throw ApiException(CustomNoteError.FORBIDDEN)
        }
        return note
    }

    private fun buildIssueResponse(note: CustomNoteEntity, code: String): CustomNoteShareIssueResponse =
        CustomNoteShareIssueResponse(
            noteId = note.id,
            title = note.title,
            ownerNickname = ownerNicknameOf(note.ownerId),
            termCount = customNoteTermRepository.countByIdNoteId(note.id),
            shareCode = code,
            shareUrl = sharingProperties.buildShareUrl(code)
        )

    private fun ownerNicknameOf(ownerId: UUID): String? =
        userRepository.findByIdOrNull(ownerId)?.nickname
}
