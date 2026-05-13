package com.snurdy.backend.domain.bookmark.service

import com.snurdy.backend.domain.bookmark.dto.BookmarkItem
import com.snurdy.backend.domain.bookmark.dto.BookmarkListResponse
import com.snurdy.backend.domain.bookmark.dto.BookmarkMutationResponse
import com.snurdy.backend.domain.bookmark.entity.BookmarkEntity
import com.snurdy.backend.domain.bookmark.entity.BookmarkId
import com.snurdy.backend.domain.bookmark.repository.BookmarkRepository
import com.snurdy.backend.domain.dictionary.glossary.entity.GlossaryEntity
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.dictionary.term.entity.TermEntity
import com.snurdy.backend.domain.dictionary.term.repository.TermRepository
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.CommonError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val glossaryRepository: GlossaryRepository,
    private val termRepository: TermRepository,
    private val userRepository: UserRepository
) {
    @Transactional(readOnly = true)
    fun getBookmarks(userId: UUID): BookmarkListResponse {
        val items = bookmarkRepository.findAllWithTermByUserId(userId)
            .map {
                BookmarkItem(
                    glossaryId = it.id.glossaryId,
                    termId = it.id.termId,
                    spelling = it.term.spelling,
                    meaning = it.term.meaning,
                    abbreviation = it.term.abbreviation,
                    createdAt = it.createdAt
                )
            }

        return BookmarkListResponse(bookmarks = items)
    }

    @Transactional
    fun addBookmark(userId: UUID, termId: Long, glossaryId: Long?): BookmarkMutationResponse {
        val user = findUser(userId)
        val term = findTerm(termId)
        val resolvedGlossaryId = resolveGlossaryId(termId, glossaryId)
        val glossary = findGlossary(resolvedGlossaryId)

        val bookmarkId = BookmarkId(
            userId = user.id,
            glossaryId = resolvedGlossaryId,
            termId = termId
        )

        if (!bookmarkRepository.existsById(bookmarkId)) {
            bookmarkRepository.save(
                BookmarkEntity(
                    id = bookmarkId,
                    user = user,
                    glossary = glossary,
                    term = term
                )
            )
        }

        return BookmarkMutationResponse(
            glossaryId = resolvedGlossaryId,
            termId = termId,
            bookmarked = true
        )
    }

    @Transactional
    fun removeBookmark(userId: UUID, termId: Long, glossaryId: Long?): BookmarkMutationResponse {
        val removedGlossaryId = if (glossaryId != null) {
            val bookmarkId = BookmarkId(userId = userId, glossaryId = glossaryId, termId = termId)
            bookmarkRepository.deleteById(bookmarkId)
            glossaryId
        } else {
            bookmarkRepository.deleteByIdUserIdAndIdTermId(userId, termId)
            null
        }

        return BookmarkMutationResponse(
            glossaryId = removedGlossaryId,
            termId = termId,
            bookmarked = false
        )
    }

    private fun findUser(userId: UUID): UserEntity =
        userRepository.findByIdOrNull(userId)
            ?: throw ApiException(CommonError.UNAUTHORIZED)

    private fun findTerm(termId: Long): TermEntity =
        termRepository.findByIdAndDeletedAtIsNull(termId)
            .orElseThrow { ApiException(DictionaryError.TERM_NOT_FOUND) }

    private fun findGlossary(glossaryId: Long): GlossaryEntity =
        glossaryRepository.findByIdOrNull(glossaryId)
            ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)

    private fun resolveGlossaryId(termId: Long, glossaryId: Long?): Long {
        if (glossaryId != null) {
            if (!glossaryTermRepository.existsByIdGlossaryIdAndIdTermId(glossaryId, termId)) {
                throw ApiException(CommonError.BAD_REQUEST, "해당 용어는 용어장에 속하지 않습니다")
            }
            return glossaryId
        }

        return glossaryTermRepository.findFirstByIdTermIdOrderByIdGlossaryIdAsc(termId)?.id?.glossaryId
            ?: throw ApiException(CommonError.BAD_REQUEST, "용어에 매핑된 용어장을 찾을 수 없습니다")
    }
}
