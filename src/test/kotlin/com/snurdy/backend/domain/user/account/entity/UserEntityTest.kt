package com.snurdy.backend.domain.user.account.entity

import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.domain.auth.external.dto.common.toEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserEntityTest {

    @Test
    fun `toEntity 변환 시 소셜 정보가 있으면 UserEntity 필드를 매핑한다`() {
        // Given
        val socialUserInfo = SocialUserInfo(
            socialId = "social-1",
            email = "snurdy@example.com",
            name = "스너디",
            nickname = "backend-learner"
        )

        // When
        val user = socialUserInfo.toEntity(SocialType.GOOGLE, "backend-learner")

        // Then
        assertEquals("social-1", user.socialId)
        assertEquals(SocialType.GOOGLE, user.socialType)
        assertEquals("snurdy@example.com", user.email)
        assertEquals("스너디", user.name)
        assertEquals("backend-learner", user.nickname)
    }

    @Test
    fun `toEntity 변환 시 닉네임 길이가 20자를 초과하면 20자로 절단한다`() {
        // Given
        val longNickname = "nickname-over-twenty-characters"
        val socialUserInfo = SocialUserInfo(socialId = "social-2")

        // When
        val user = socialUserInfo.toEntity(SocialType.APPLE, longNickname)

        // Then
        assertEquals(MAX_NICKNAME_LENGTH, requireNotNull(user.nickname).length)
        assertEquals(longNickname.take(MAX_NICKNAME_LENGTH), user.nickname)
    }

    @Test
    fun `toEntity 변환 시 name 이 blank 면 null 로 정규화한다`() {
        // Given
        val socialUserInfo = SocialUserInfo(
            socialId = "social-3",
            email = "blank-name@example.com",
            name = "   "
        )

        // When
        val user = socialUserInfo.toEntity(SocialType.KAKAO, "잔잔한 고래")

        // Then
        assertEquals(null, user.name)
        assertEquals("잔잔한 고래", user.nickname)
    }

    companion object {
        private const val MAX_NICKNAME_LENGTH = 20
    }
}
