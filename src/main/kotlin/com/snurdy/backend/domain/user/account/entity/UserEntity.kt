package com.snurdy.backend.domain.user.account.entity

import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_social_active",
            columnNames = ["active_social_id", "social_type"]
        )
    ]
)
class UserEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "social_id", nullable = false)
    val socialId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    val socialType: SocialType,

    @Column(nullable = true)
    var email: String? = null,

    @Column(nullable = true, length = 50)
    var name: String? = null,

    @Column(nullable = true, length = 20)
    var nickname: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: Role = Role.USER,

    @Column(nullable = false)
    var currentStreak: Int = 0,

    @Column(nullable = false)
    var totalLearningTerms: Int = 0,

    @Column(nullable = false)
    var longestStreak: Int = 0,

    @Column(name = "migration_declined_at")
    var migrationDeclinedAt: Instant? = null,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,

    @Column(name = "profile_image_url", length = 512)
    var profileImageUrl: String? = null,

    @Column(name = "marketing_consent", nullable = false)
    var marketingConsent: Boolean = false,

    @Column(name = "marketing_consent_at")
    var marketingConsentAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "default_test_type", length = 32)
    var defaultTestType: TestType? = null
) : BaseTimeEntity() {

    fun isDeleted(): Boolean = deletedAt != null

    fun softDelete() {
        deletedAt = Instant.now()
    }

    fun declineMigration() {
        migrationDeclinedAt = Instant.now()
    }

    fun updateNickname(nickname: String?) {
        this.nickname = nickname?.take(MAX_NICKNAME_LENGTH)
    }

    fun updateProfileImageUrl(url: String?) {
        this.profileImageUrl = url?.take(MAX_PROFILE_IMAGE_URL_LENGTH)
    }

    // 마케팅 동의 토글
    // 동의 시 동의 시각도 함께 기록
    // 철회 시 시각은 null로 비움
    fun setMarketingConsent(agreed: Boolean, now: Instant) {
        this.marketingConsent = agreed
        this.marketingConsentAt = if (agreed) now else null
    }

    /**
     * 학습 이벤트 집계 결과를 반영.
     * streak 값은 호출자가 계산(증분 혹은 전체 재계산)해서 전달한다.
     */
    fun applyLearningOutcome(learnedDelta: Int, newCurrentStreak: Int) {
        if (learnedDelta > 0) {
            totalLearningTerms += learnedDelta
        }
        currentStreak = newCurrentStreak.coerceAtLeast(0)
        longestStreak = maxOf(longestStreak, currentStreak)
    }

    /** 마이그레이션 시 요약 지표를 덮어쓴다. */
    fun replaceSummaryFromMigration(currentStreakValue: Int, longestStreakValue: Int, totalLearning: Int) {
        val normalizedCurrent = currentStreakValue.coerceAtLeast(0)
        val normalizedLongest = longestStreakValue.coerceAtLeast(0)
        currentStreak = normalizedCurrent
        longestStreak = maxOf(normalizedLongest, normalizedCurrent)
        totalLearningTerms = totalLearning.coerceAtLeast(0)
    }

    companion object {
        internal const val MAX_NICKNAME_LENGTH = 20
        private const val MAX_PROFILE_IMAGE_URL_LENGTH = 512
    }
}
