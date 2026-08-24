package com.snurdy.backend.domain.user.account.entity

import com.snurdy.backend.global.entity.BaseTimeEntity
import com.snurdy.backend.global.extension.normalizeToNull
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
    var marketingConsentAt: Instant? = null
) : BaseTimeEntity() {

    fun isDeleted(): Boolean = deletedAt != null

    fun softDelete() {
        deletedAt = Instant.now()
    }

    fun declineMigration() {
        migrationDeclinedAt = Instant.now()
    }

    // 빈값이면 대입하지 않아 기존 닉네임을 보존. nickname 은 항상 값이 있어야 하므로 null 로 밀지 않음
    fun updateNickname(nickname: String?) {
        nickname.normalizeToNull()?.let { this.nickname = it.take(MAX_NICKNAME_LENGTH) }
    }

    fun updateProfileImageUrl(url: String?) {
        this.profileImageUrl = url?.take(MAX_PROFILE_IMAGE_URL_LENGTH)
    }

    // 초기 가입 시 소셜에서 이메일을 못 받은 유저를 재로그인 시점에 채운다. 이미 있으면 덮지 않음
    fun backfillEmailIfMissing(email: String?) {
        if (this.email == null) {
            this.email = email.normalizeToNull()
        }
    }

    // 마케팅 동의 토글
    // 동의 시 동의 시각도 함께 기록
    // 철회 시 시각은 null로 비움
    fun setMarketingConsent(agreed: Boolean, now: Instant) {
        this.marketingConsent = agreed
        this.marketingConsentAt = if (agreed) now else null
    }

    // 진도 이벤트 집계 결과를 반영. streak 값은 호출자가 계산해서 전달
    fun applyProgressOutcome(learnedDelta: Int, newCurrentStreak: Int) {
        if (learnedDelta > 0) {
            totalLearningTerms += learnedDelta
        }
        currentStreak = newCurrentStreak.coerceAtLeast(0)
        longestStreak = maxOf(longestStreak, currentStreak)
    }

    // 마이그레이션 시 요약 지표를 덮어쓴다
    fun replaceSummaryFromMigration(currentStreakValue: Int, longestStreakValue: Int, totalLearningTerms: Int) {
        val normalizedCurrent = currentStreakValue.coerceAtLeast(0)
        val normalizedLongest = longestStreakValue.coerceAtLeast(0)
        currentStreak = normalizedCurrent
        longestStreak = maxOf(normalizedLongest, normalizedCurrent)
        this.totalLearningTerms = totalLearningTerms.coerceAtLeast(0)
    }

    companion object {
        internal const val MAX_NICKNAME_LENGTH = 20
        private const val MAX_PROFILE_IMAGE_URL_LENGTH = 512
    }
}
