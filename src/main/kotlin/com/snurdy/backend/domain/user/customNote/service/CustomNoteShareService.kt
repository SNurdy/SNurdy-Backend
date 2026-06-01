package com.snurdy.backend.domain.user.customNote.service

import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.domain.user.customNote.config.SharingProperties
import com.snurdy.backend.domain.user.customNote.dto.CustomNoteShareIssueResponse
import com.snurdy.backend.domain.user.customNote.entity.CustomNoteEntity
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteRepository
import com.snurdy.backend.domain.user.customNote.repository.CustomNoteTermRepository
import com.snurdy.backend.global.constants.CustomNoteError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CustomNoteShareService(
    private val customNoteRepository: CustomNoteRepository,
    private val customNoteTermRepository: CustomNoteTermRepository,
    private val userRepository: UserRepository,
    private val shareCodeGenerator: ShareCodeGenerator,
    private val sharingProperties: SharingProperties
) {

    // 본인 단어장의 공유 코드 발급. 이미 활성 코드 있으면 SHARE_CODE_ALREADY_EXISTS
    @Transactional
    fun issue(userId: UUID, noteId: Long): CustomNoteShareIssueResponse {
        val note = findOwned(userId, noteId)
        if (note.isShareActive()) {
            throw ApiException(CustomNoteError.SHARE_CODE_ALREADY_EXISTS)
        }

        val newCode = shareCodeGenerator.generateUnique()
        note.issueShareCode(newCode)

        return buildResponse(note)
    }

    // 본인 단어장의 활성 공유 코드 조회. 없으면 SHARE_CODE_NOT_FOUND
    @Transactional(readOnly = true)
    fun get(userId: UUID, noteId: Long): CustomNoteShareIssueResponse {
        val note = findOwned(userId, noteId)
        if (!note.isShareActive()) {
            throw ApiException(CustomNoteError.SHARE_CODE_NOT_FOUND)
        }
        return buildResponse(note)
    }

    // 공유 비활성화. shareCode = null 처리. 멱등 (이미 null 이면 그대로)
    @Transactional
    fun revoke(userId: UUID, noteId: Long) {
        val note = findOwned(userId, noteId)
        if (note.isShareActive()) {
            note.revokeShare()
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

    private fun buildResponse(note: CustomNoteEntity): CustomNoteShareIssueResponse {
        val code = note.shareCode ?: throw ApiException(CustomNoteError.SHARE_CODE_NOT_FOUND)
        return CustomNoteShareIssueResponse(
            noteId = note.id,
            title = note.title,
            ownerNickname = userRepository.findByIdOrNull(note.ownerId)?.nickname,
            termCount = customNoteTermRepository.countByIdNoteId(note.id),
            shareCode = code,
            shareUrl = sharingProperties.buildShareUrl(code)
        )
    }
}
