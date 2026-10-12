package com.snurdy.backend.domain.test.glossary.service

import com.snurdy.backend.domain.dictionary.glossary.GlossaryProgressCalculator
import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryTermRepository
import com.snurdy.backend.domain.progress.entity.DailyProgressId
import com.snurdy.backend.domain.progress.entity.GlossaryProgressId
import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.progress.repository.GlossaryProgressRepository
import com.snurdy.backend.domain.test.glossary.dto.GlossaryTestProgressSnapshot
import com.snurdy.backend.global.constants.APP_ZONE_ID
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

// 진도 반영 전후에 같은 방식으로 읽어야 비교 결과가 어긋나지 않으므로 지표 조회를 한곳에 모은다
@Component
class GlossaryTestSnapshotReader(
    private val glossaryTermRepository: GlossaryTermRepository,
    private val glossaryProgressRepository: GlossaryProgressRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val clock: Clock
) {

    // 사용자 행 잠금 이후에 호출해야 선행 제출이 커밋한 진도를 기준으로 읽는다
    fun readSnapshot(userId: UUID, glossaryId: Long): GlossaryTestProgressSnapshot {
        val progressRate = readProgressRate(userId, glossaryId)
        val today = clock.instant().atZone(APP_ZONE_ID).toLocalDate()
        val todayProgress = dailyProgressRepository.findByIdOrNull(DailyProgressId(userId, today))

        return GlossaryTestProgressSnapshot(
            progressRate = progressRate,
            level = GlossaryProgressCalculator.levelFromRate(progressRate),
            activeToday = todayProgress?.let { (it.newLearnedCount + it.allLearnedCount) > 0 } ?: false,
            todayStudiedCount = todayProgress?.allLearnedCount ?: 0,
            totalStudiedCount = dailyProgressRepository.sumAllLearnedCountByUserId(userId).toInt()
        )
    }

    // 단어장에 아직 학습 기록이 없으면 학습 수 0 으로 계산한다
    private fun readProgressRate(userId: UUID, glossaryId: Long): Double {
        val totalTermCount = glossaryTermRepository.countByIdGlossaryId(glossaryId)
        val learnedCount = glossaryProgressRepository
            .findByIdOrNull(GlossaryProgressId(userId, glossaryId))
            ?.learnedCount ?: 0
        return GlossaryProgressCalculator.progressRate(learnedCount, totalTermCount)
    }
}