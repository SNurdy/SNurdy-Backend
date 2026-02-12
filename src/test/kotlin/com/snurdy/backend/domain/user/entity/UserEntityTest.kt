package com.snurdy.backend.domain.user.entity

import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserEntityTest {

    @Test
    fun `from 생성 시 소셜 정보가 있으면 UserEntity 필드를 매핑한다`() {
        // Given
        val socialUserInfo = SocialUserInfo(
            socialId = "social-1",
            email = "snurdy@example.com",
            name = "스너디",
            nickname = "backend-learner"
        )

        // When
        val user = UserEntity.from(socialUserInfo, SocialType.GOOGLE)

        // Then
        assertEquals("social-1", user.socialId)
        assertEquals(SocialType.GOOGLE, user.socialType)
        assertEquals("snurdy@example.com", user.email)
        assertEquals("스너디", user.name)
        assertEquals("backend-learner", user.nickname)
    }

    @Test
    fun `from 생성 시 닉네임 길이가 20자를 초과하면 20자로 절단한다`() {
        // Given
        val longNickname = "nickname-over-twenty-characters"
        val socialUserInfo = SocialUserInfo(
            socialId = "social-2",
            nickname = longNickname
        )

        // When
        val user = UserEntity.from(socialUserInfo, SocialType.APPLE)

        // Then
        assertEquals(MAX_NICKNAME_LENGTH, user.nickname.length)
        assertEquals(longNickname.take(MAX_NICKNAME_LENGTH), user.nickname)
    }

    @Test
    fun `from 생성 시 닉네임이 없으면 빈 문자열을 기본값으로 사용한다`() {
        // Given
        val socialUserInfo = SocialUserInfo(
            socialId = "social-3",
            email = "no-nickname@example.com"
        )

        // When
        val user = UserEntity.from(socialUserInfo, SocialType.KAKAO)

        // Then
        assertEquals("", user.nickname)
    }

    companion object {
        private const val MAX_NICKNAME_LENGTH = 20
    }
}
