package com.snurdy.backend.domain.migration.service

import com.snurdy.backend.domain.dictionary.term.repository.TermIdAliasRepository
import com.snurdy.backend.domain.migration.dto.BookmarkPayload
import com.snurdy.backend.domain.migration.dto.MigrationUploadRequest
import com.snurdy.backend.domain.migration.dto.TermProgressPayload
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MigrationTermIdAliasService(
    private val termIdAliasRepository: TermIdAliasRepository
) {

    // 최근 복습 상태가 다음 복습 일정을 결정하므로 SRS 필드를 섞지 않고 기록 하나를 통째로 남김
    private val termProgressPriority: Comparator<TermProgressCandidate> =
        compareBy<TermProgressCandidate, Instant?>(nullsFirst<Instant>()) { it.payload.lastReviewedAt }
            .thenBy { it.payload.reviewCount }
            .thenBy { it.sentAsCanonical }

    fun replaceAliasTermIds(request: MigrationUploadRequest): MigrationTermIdAliasResult {
        val canonicalTermIdByAlias = loadCanonicalTermIdByAlias(request)
        return MigrationTermIdAliasResult(
            termProgresses = mergeTermProgresses(request.termProgresses, canonicalTermIdByAlias),
            bookmarks = mergeBookmarks(request.bookmarks, canonicalTermIdByAlias)
        )
    }

    private fun loadCanonicalTermIdByAlias(request: MigrationUploadRequest): Map<Long, Long> {
        val requestedTermIds = (request.termProgresses.map { it.termId } + request.bookmarks.map { it.termId }).distinct()
        if (requestedTermIds.isEmpty()) return emptyMap()

        return termIdAliasRepository
            .findByAliasTermIdIn(requestedTermIds)
            .associate { it.aliasTermId to it.canonicalTermId }
    }

    private fun mergeTermProgresses(
        payloads: List<TermProgressPayload>,
        canonicalTermIdByAlias: Map<Long, Long>
    ): List<TermProgressPayload> =
        payloads
            .map { payload ->
                val canonicalTermId = canonicalTermIdByAlias[payload.termId]
                TermProgressCandidate(
                    payload = payload.copy(termId = canonicalTermId ?: payload.termId),
                    sentAsCanonical = canonicalTermId == null
                )
            }
            .groupBy { it.payload.glossaryId to it.payload.termId }
            .values
            .map { candidates -> candidates.maxWith(termProgressPriority).payload }

    private fun mergeBookmarks(
        payloads: List<BookmarkPayload>,
        canonicalTermIdByAlias: Map<Long, Long>
    ): List<BookmarkPayload> =
        payloads
            .map { payload -> payload.copy(termId = canonicalTermIdByAlias[payload.termId] ?: payload.termId) }
            .distinctBy { it.glossaryId to it.termId }

    private data class TermProgressCandidate(
        val payload: TermProgressPayload,
        val sentAsCanonical: Boolean
    )
}

data class MigrationTermIdAliasResult(
    val termProgresses: List<TermProgressPayload>,
    val bookmarks: List<BookmarkPayload>
)