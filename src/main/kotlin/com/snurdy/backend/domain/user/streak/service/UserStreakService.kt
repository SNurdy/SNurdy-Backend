package com.snurdy.backend.domain.user.streak.service

import com.snurdy.backend.domain.progress.repository.DailyProgressRepository
import com.snurdy.backend.domain.user.streak.dto.UserStreakResponse
import com.snurdy.backend.domain.user.streak.dto.toStreakResponse
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import com.snurdy.backend.global.constants.APP_ZONE_ID
import java.util.UUID

// 사용자 학습 streak 조회 책임 전담
// 저장된 streak 값이 만료되었는지 daily progress 로 판정하여 effective streak 반환
@Service
class UserStreakService(
    private val userRepository: UserRepository,
    private val dailyProgressRepository: DailyProgressRepository,
    private val clock: Clock
) {

    @Transactional(readOnly = true)
    fun getStreak(userId: UUID): UserStreakResponse {
        val user = findUser(userId)
        val effectiveCurrent = if (isStreakStillAlive(userId, user.currentStreak)) {
            user.currentStreak
        } else {
            0
        }
        return user.toStreakResponse(effectiveCurrent)
    }

    // 저장된 currentStreak 가 현 시점에서도 유효한지 판정
    // 저장값이 0 이면 비교할 필요 없음
    // 오늘 또는 어제 활성이면 streak 유지 (자정 전까지는 어제 활성도 회복 기회로 인정)
    // 그 외(그제 이전이 마지막 활성)면 만료
    // DB 는 갱신하지 않는다 (read-only). 다음 학습 제출 시 정상 경로로 재계산된다
    private fun isStreakStillAlive(userId: UUID, storedStreak: Int): Boolean {
        if (storedStreak == 0) return true
        val today = today()
        val yesterday = today.minusDays(1)
        val recent = dailyProgressRepository
            .findAllByIdUserIdAndIdDateIn(userId, listOf(today, yesterday))
        return recent.any { (it.newLearnedCount + it.allLearnedCount) > 0 }
    }

    private fun today(): LocalDate = LocalDate.now(clock.withZone(APP_ZONE_ID))

    private fun findUser(userId: UUID): UserEntity =
        userRepository.findByIdOrNull(userId) ?: throw ApiException(UserError.ACCOUNT_NOT_FOUND)
}
