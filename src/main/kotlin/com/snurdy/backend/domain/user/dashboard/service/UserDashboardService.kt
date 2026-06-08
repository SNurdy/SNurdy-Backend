package com.snurdy.backend.domain.user.dashboard.service

import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryAvailableCountProjection
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.progress.entity.GlossaryProgressEntity
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.progress.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.progress.repository.TermProgressRepository
import com.snurdy.backend.domain.user.dashboard.dto.DashboardDailyStatistic
import com.snurdy.backend.domain.user.dashboard.dto.DashboardGlossaryListItem
import com.snurdy.backend.domain.user.dashboard.dto.DashboardGlossaryListResponse
import com.snurdy.backend.domain.user.dashboard.dto.DashboardGlossaryPeriod
import com.snurdy.backend.domain.user.dashboard.dto.DashboardGlossaryStatisticItem
import com.snurdy.backend.domain.user.dashboard.dto.DashboardGlossaryStatisticsResponse
import com.snurdy.backend.domain.user.dashboard.dto.DashboardSelectedGlossary
import com.snurdy.backend.domain.user.dashboard.dto.DashboardStreak
import com.snurdy.backend.domain.user.dashboard.dto.DashboardWeekStatistics
import com.snurdy.backend.domain.user.dashboard.dto.UserDashboardResponse
import com.snurdy.backend.domain.user.preference.service.UserPreferenceService
import com.snurdy.backend.domain.user.streak.service.UserStreakService
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.domain.user.wrongAnswerNote.repository.WrongAnswerTermRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import com.snurdy.backend.global.constants.APP_ZONE_ID
import java.time.temporal.TemporalAdjusters
import java.util.UUID

// 마이페이지 대시보드 BFF
// 메인 화면 + 과 변경 바텀시트 + 과 지표 상세 3개 endpoint 의 응답을 책임진다
@Service
class UserDashboardService(
    private val userStreakService: UserStreakService,
    private val userPreferenceService: UserPreferenceService,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val glossaryRepository: GlossaryRepository,
    private val glossaryTermRepository: GlossaryTermRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val termProgressRepository: TermProgressRepository,
    private val wrongAnswerTermRepository: WrongAnswerTermRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun getDashboard(userId: UUID, startDate: LocalDate?): UserDashboardResponse {
        val today = LocalDate.now(clock.withZone(APP_ZONE_ID))
        // 미지정 시 오늘 포함 직전 7일 (today 가 끝), 지정 시 startDate ~ startDate+6
        val resolvedStart = startDate ?: today.minusDays(DEFAULT_PERIOD_DAYS - 1)
        val resolvedEnd = resolvedStart.plusDays(DEFAULT_PERIOD_DAYS - 1)

        val streakResponse = userStreakService.getStreak(userId)
        val selectedGlossaryId = userPreferenceService.getPreference(userId).selectedGlossaryId
        val selectedGlossary = glossaryRepository.findGlossaryTermCounts()
            .firstOrNull { it.glossaryId == selectedGlossaryId }
            ?.let { meta ->
                val progress = glossaryProgressRepository.findAllByIdUserId(userId)
                    .firstOrNull { it.id.glossaryId == selectedGlossaryId }
                val learnedCount = progress?.learnedCount ?: 0
                val progressRate = GlossaryProgressCalculator.progressRate(learnedCount, meta.termCount)
                DashboardSelectedGlossary(
                    glossaryId = meta.glossaryId,
                    category = meta.category,
                    title = meta.title,
                    learnedCount = learnedCount,
                    totalTermCount = meta.termCount,
                    progressRate = progressRate,
                    level = GlossaryProgressCalculator.levelFromRate(progressRate),
                    lastLearnedAt = progress?.lastLearnedAt,
                    availableCounts = buildAvailableCounts(glossaryTermRepository.countAvailableTerms(meta.glossaryId))
                )
            } ?: throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        val weekStatistics = buildWeekStatistics(userId, resolvedStart, resolvedEnd)

        return UserDashboardResponse(
            streak = DashboardStreak(
                longestStreak = streakResponse.longestStreak,
                totalLearningTerms = streakResponse.totalLearningTerms
            ),
            selectedGlossary = selectedGlossary,
            weekStatistics = weekStatistics,
            generatedAt = clock.instant()
        )
    }

    @Transactional(readOnly = true)
    fun listGlossaries(userId: UUID): DashboardGlossaryListResponse {
        val catalog = glossaryRepository.findGlossaryTermCounts()
        val progressByGlossaryId = glossaryProgressRepository.findAllByIdUserId(userId)
            .associateBy { it.id.glossaryId }

        val items = catalog.map { meta ->
            val progress = progressByGlossaryId[meta.glossaryId]
            val learnedCount = progress?.learnedCount ?: 0
            val progressRate = GlossaryProgressCalculator.progressRate(learnedCount, meta.termCount)
            DashboardGlossaryListItem(
                glossaryId = meta.glossaryId,
                category = meta.category,
                title = meta.title,
                learnedCount = learnedCount,
                totalTermCount = meta.termCount,
                progressRate = progressRate,
                level = GlossaryProgressCalculator.levelFromRate(progressRate),
                lastLearnedAt = progress?.lastLearnedAt
            )
        }.sortedWith(
            compareByDescending<DashboardGlossaryListItem> { it.lastLearnedAt != null }
                .thenByDescending { it.lastLearnedAt }
                .thenBy { it.glossaryId }
        )

        return DashboardGlossaryListResponse(glossaries = items)
    }

    // 과 지표 리스트 학습 시작한 과만 lastLearnedAt 최근순
    @Transactional(readOnly = true)
    fun listGlossaryStatistics(
        userId: UUID,
        period: DashboardGlossaryPeriod
    ): DashboardGlossaryStatisticsResponse {
        val progresses = glossaryProgressRepository.findAllByIdUserId(userId)
        if (progresses.isEmpty()) {
            return DashboardGlossaryStatisticsResponse(period = period, glossaries = emptyList())
        }

        val glossaryIds = progresses.map { it.id.glossaryId }
        val metaByGlossaryId = glossaryRepository.findGlossaryTermCounts()
            .filter { it.glossaryId in glossaryIds }
            .associateBy { it.glossaryId }

        val after: Instant? = when (period) {
            DashboardGlossaryPeriod.WEEK -> weekStartInstant()
            DashboardGlossaryPeriod.ALL -> null
        }

        val learnedCountByGlossaryId = termProgressRepository
            .countLearnedSinceByGlossaries(userId, glossaryIds, after)
            .associate { it.glossaryId to it.termCount }
        val wrongCountByGlossaryId = wrongAnswerTermRepository
            .countActiveByGlossaries(userId, glossaryIds, after)
            .associate { it.glossaryId to it.wrongCount }

        val items = progresses
            .sortedWith(
                compareByDescending<GlossaryProgressEntity> { it.lastLearnedAt != null }
                    .thenByDescending { it.lastLearnedAt }
                    .thenBy { it.id.glossaryId }
            )
            .mapNotNull { progress ->
                val glossaryId = progress.id.glossaryId
                val meta = metaByGlossaryId[glossaryId] ?: return@mapNotNull null
                val progressRate = GlossaryProgressCalculator.progressRate(progress.learnedCount, meta.termCount)
                DashboardGlossaryStatisticItem(
                    glossaryId = glossaryId,
                    category = meta.category,
                    title = meta.title,
                    learnedCount = progress.learnedCount,
                    totalTermCount = meta.termCount,
                    progressRate = progressRate,
                    level = GlossaryProgressCalculator.levelFromRate(progressRate),
                    periodLearnedTermCount = learnedCountByGlossaryId[glossaryId] ?: 0L,
                    periodWrongAnswerCount = wrongCountByGlossaryId[glossaryId] ?: 0L
                )
            }

        return DashboardGlossaryStatisticsResponse(period = period, glossaries = items)
    }

    // testType 별 가용 단어 수 매핑
    // abbreviation 계열 테스트는 abbreviation 보유 단어 수, 그 외는 전체 단어 수
    private fun buildAvailableCounts(counts: GlossaryAvailableCountProjection): Map<TestType, Int> {
        val total = counts.totalCount.toInt()
        val abbreviation = counts.abbreviationCount.toInt()
        return TestType.values().associateWith { type ->
            when (type) {
                TestType.WRITE_ABBREVIATION, TestType.MC_PICK_ABBR -> abbreviation
                else -> total
            }
        }
    }

    // 7일치 데이터 빌드. 학습 안 한 날은 0 으로 채워 항상 7개 반환
    private fun buildWeekStatistics(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): DashboardWeekStatistics {
        val rows = dailyProgressRepository
            .findByIdUserIdAndIdDateBetweenOrderByIdDateAsc(userId, startDate, endDate)
            .associateBy { it.id.date }

        val days = (0 until DEFAULT_PERIOD_DAYS).map { offset ->
            val date = startDate.plusDays(offset)
            val row = rows[date]
            DashboardDailyStatistic(
                date = date,
                dayOfWeek = date.dayOfWeek,
                learnedCount = row?.learnedCount ?: 0,
                completedCount = row?.completedCount ?: 0
            )
        }

        return DashboardWeekStatistics(
            startDate = startDate,
            endDate = endDate,
            days = days
        )
    }

    private fun weekStartInstant(): Instant {
        val today = LocalDate.now(clock.withZone(APP_ZONE_ID))
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.atTime(LocalTime.MIN).atZone(APP_ZONE_ID).toInstant()
    }

    companion object {
        private const val DEFAULT_PERIOD_DAYS: Long = 7
    }
}
