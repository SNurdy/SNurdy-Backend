package com.snurdy.backend.domain.user.termMemo.service

import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.domain.user.termMemo.dto.TermMemoResponse
import com.snurdy.backend.domain.user.termMemo.dto.toResponse
import com.snurdy.backend.domain.user.termMemo.entity.UserTermMemoEntity
import com.snurdy.backend.domain.user.termMemo.entity.UserTermMemoId
import com.snurdy.backend.domain.user.termMemo.repository.UserTermMemoRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.TermMemoError
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TermMemoService(
    private val userTermMemoRepository: UserTermMemoRepository,
    private val userRepository: UserRepository,
    private val termRepository: TermRepository
) {

    @Transactional
    fun upsert(userId: UUID, termId: Long, content: String): TermMemoResponse {
        val existing = userTermMemoRepository.findByIdUserIdAndIdTermId(userId, termId)
        val saved = if (existing != null) {
            existing.updateContent(content)
            userTermMemoRepository.save(existing)
        } else {
            val user = userRepository.findByIdOrNull(userId)
                ?: throw ApiException(UserError.ACCOUNT_NOT_FOUND)
            val term = termRepository.findByIdAndDeletedAtIsNull(termId)
                .orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }
            userTermMemoRepository.save(
                UserTermMemoEntity(
                    id = UserTermMemoId(userId = userId, termId = termId),
                    user = user,
                    term = term,
                    content = content
                )
            )
        }
        return saved.toResponse()
    }

    @Transactional
    fun delete(userId: UUID, termId: Long) {
        if (!userTermMemoRepository.existsByIdUserIdAndIdTermId(userId, termId)) {
            throw ApiException(TermMemoError.NOT_FOUND)
        }
        userTermMemoRepository.deleteByIdUserIdAndIdTermId(userId, termId)
    }
}
