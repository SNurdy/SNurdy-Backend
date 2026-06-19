package com.snurdy.backend.domain.user.wrongAnswerNote.repository

import com.snurdy.backend.domain.user.customNote.repository.NoteCardTermProjection
import com.snurdy.backend.domain.user.wrongAnswerNote.entity.WrongAnswerTermEntity
import com.snurdy.backend.domain.user.wrongAnswerNote.entity.WrongAnswerTermId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface WrongAnswerTermRepository : JpaRepository<WrongAnswerTermEntity, WrongAnswerTermId> {

    fun findAllByIdUserIdAndIdTermIdInAndDeletedAtIsNull(
        userId: UUID,
        termIds: Collection<Long>
    ): List<WrongAnswerTermEntity>

    // soft delete 무관 — recordIncorrects 가 deletedAt 상태와 관계 없이 row 찾아 복구
    fun findAllByIdUserIdAndIdTermIdIn(
        userId: UUID,
        termIds: Collection<Long>
    ): List<WrongAnswerTermEntity>

    // 기간별 카드 카운트. after 가 null 이면 ALL
    @Query(
        """
        SELECT COUNT(w) FROM WrongAnswerTermEntity w
         WHERE w.id.userId = :userId
           AND w.deletedAt IS NULL
           AND (:after IS NULL OR w.incorrectAt >= :after)
        """
    )
    fun countActive(
        @Param("userId") userId: UUID,
        @Param("after") after: Instant?
    ): Long

    // 대시보드 과 지표 리스트의 과별 오답 단어 종 수 batch. N+1 회피
    @Query(
        """
        SELECT w.glossaryId AS glossaryId, COUNT(w) AS wrongCount
          FROM WrongAnswerTermEntity w
         WHERE w.id.userId = :userId
           AND w.glossaryId IN :glossaryIds
           AND w.deletedAt IS NULL
           AND (:after IS NULL OR w.incorrectAt >= :after)
         GROUP BY w.glossaryId
        """
    )
    fun countActiveByGlossaries(
        @Param("userId") userId: UUID,
        @Param("glossaryIds") glossaryIds: Collection<Long>,
        @Param("after") after: Instant?
    ): List<GlossaryWrongCountProjection>

    // 과별 그루핑 카운트 (glossary_id 별 단어 수)
    @Query(
        """
        SELECT w.glossaryId AS glossaryId, COUNT(w) AS incorrectTermCount
          FROM WrongAnswerTermEntity w
         WHERE w.id.userId = :userId
           AND w.deletedAt IS NULL
           AND w.glossaryId IS NOT NULL
         GROUP BY w.glossaryId
        """
    )
    fun groupByGlossary(@Param("userId") userId: UUID): List<GlossaryWrongAnswerGroup>

    // 활성 단어 목록 — 검색어 없음 (incorrect_at DESC)
    @Query(
        value = """
        SELECT t.id AS id,
               wat.glossary_id AS glossaryId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               wat.total_incorrect_count AS totalIncorrectCount
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.deleted_at IS NULL
           AND (:after IS NULL OR wat.incorrect_at >= :after)
           AND (:glossaryId IS NULL OR wat.glossary_id = :glossaryId)
         ORDER BY wat.incorrect_at DESC, t.id ASC
        """,
        countQuery = """
        SELECT COUNT(*)
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.deleted_at IS NULL
           AND (:after IS NULL OR wat.incorrect_at >= :after)
           AND (:glossaryId IS NULL OR wat.glossary_id = :glossaryId)
        """,
        nativeQuery = true
    )
    fun findActiveTerms(
        @Param("userId") userId: UUID,
        @Param("after") after: Instant?,
        @Param("glossaryId") glossaryId: Long?,
        pageable: Pageable
    ): Page<WrongAnswerTermItemProjection>

    // 활성 단어 목록 — 검색어 있음 (FULLTEXT MATCH AGAINST, relevance DESC)
    @Query(
        value = """
        SELECT t.id AS id,
               wat.glossary_id AS glossaryId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               wat.total_incorrect_count AS totalIncorrectCount
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.deleted_at IS NULL
           AND (:after IS NULL OR wat.incorrect_at >= :after)
           AND (:glossaryId IS NULL OR wat.glossary_id = :glossaryId)
           AND MATCH(t.spelling, t.meaning, t.abbreviation) AGAINST(:query IN BOOLEAN MODE)
         ORDER BY MATCH(t.spelling, t.meaning, t.abbreviation) AGAINST(:query IN BOOLEAN MODE) DESC,
                  wat.incorrect_at DESC,
                  t.id ASC
        """,
        countQuery = """
        SELECT COUNT(*)
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.deleted_at IS NULL
           AND (:after IS NULL OR wat.incorrect_at >= :after)
           AND (:glossaryId IS NULL OR wat.glossary_id = :glossaryId)
           AND MATCH(t.spelling, t.meaning, t.abbreviation) AGAINST(:query IN BOOLEAN MODE)
        """,
        nativeQuery = true
    )
    fun searchActiveTerms(
        @Param("userId") userId: UUID,
        @Param("after") after: Instant?,
        @Param("glossaryId") glossaryId: Long?,
        @Param("query") query: String,
        pageable: Pageable
    ): Page<WrongAnswerTermItemProjection>

    // 카드용 단어 전체 조회 — 기간 필터 (after IS NULL 이면 ALL). RAND 정렬 (카드 노출 순서)
    // requireAbbreviation = true 면 abbreviation 보유 단어만
    @Query(
        value = """
        SELECT t.id AS termId,
               wat.glossary_id AS glossaryId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               t.explanation AS explanation
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.deleted_at IS NULL
           AND (:after IS NULL OR wat.incorrect_at >= :after)
           AND (:requireAbbreviation = FALSE
                OR (t.abbreviation IS NOT NULL AND t.abbreviation <> ''))
         ORDER BY RAND()
        """,
        nativeQuery = true
    )
    fun findCardTermsByPeriod(
        @Param("userId") userId: UUID,
        @Param("after") after: Instant?,
        @Param("requireAbbreviation") requireAbbreviation: Boolean
    ): List<NoteCardTermProjection>

    // 카드용 단어 전체 조회 — 과 필터 (glossaryId). RAND 정렬
    @Query(
        value = """
        SELECT t.id AS termId,
               wat.glossary_id AS glossaryId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               t.explanation AS explanation
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.deleted_at IS NULL
           AND wat.glossary_id = :glossaryId
           AND (:requireAbbreviation = FALSE
                OR (t.abbreviation IS NOT NULL AND t.abbreviation <> ''))
         ORDER BY RAND()
        """,
        nativeQuery = true
    )
    fun findCardTermsByGlossary(
        @Param("userId") userId: UUID,
        @Param("glossaryId") glossaryId: Long,
        @Param("requireAbbreviation") requireAbbreviation: Boolean
    ): List<NoteCardTermProjection>

    // distractor 출처 결정용 — termId 별 glossaryId 조회 (active row 만)
    @Query(
        value = """
        SELECT wat.term_id AS termId,
               wat.glossary_id AS glossaryId
          FROM wrong_answer_terms wat
         WHERE wat.user_id = :userId
           AND wat.term_id IN :termIds
           AND wat.deleted_at IS NULL
        """,
        nativeQuery = true
    )
    fun findGlossaryIdsByUserAndTermIds(
        @Param("userId") userId: UUID,
        @Param("termIds") termIds: Collection<Long>
    ): List<WrongAnswerTermGlossaryProjection>

    // 자주 틀린 단어 카드용 — 선택 단어장 내에서 minCount 이상 오답 풀에서 랜덤 1개
    // 학습 Home 진입마다 다른 단어 노출 (학습 효과)
    @Query(
        value = """
        SELECT t.id AS termId,
               t.spelling AS spelling,
               t.meaning AS meaning,
               t.abbreviation AS abbreviation,
               wat.total_incorrect_count AS totalIncorrectCount
          FROM wrong_answer_terms wat
          JOIN terms t ON t.id = wat.term_id AND t.deleted_at IS NULL
         WHERE wat.user_id = :userId
           AND wat.glossary_id = :glossaryId
           AND wat.deleted_at IS NULL
           AND wat.total_incorrect_count >= :minCount
         ORDER BY RAND()
         LIMIT 1
        """,
        nativeQuery = true
    )
    fun findRandomFrequentlyIncorrect(
        @Param("userId") userId: UUID,
        @Param("glossaryId") glossaryId: Long,
        @Param("minCount") minCount: Int
    ): FrequentlyIncorrectTermProjection?

}

interface FrequentlyIncorrectTermProjection {
    val termId: Long
    val spelling: String
    val meaning: String
    val abbreviation: String?
    val totalIncorrectCount: Int
}

interface WrongAnswerTermItemProjection {
    val id: Long
    val glossaryId: Long
    val spelling: String
    val meaning: String
    val abbreviation: String?
    val totalIncorrectCount: Int
}

interface GlossaryWrongAnswerGroup {
    val glossaryId: Long
    val incorrectTermCount: Long
}

interface GlossaryWrongCountProjection {
    val glossaryId: Long
    val wrongCount: Long
}

interface WrongAnswerTermGlossaryProjection {
    val termId: Long
    val glossaryId: Long
}
