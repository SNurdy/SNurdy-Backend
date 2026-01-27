package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.api.auth.client.SocialProvider
import com.snurdy.backend.api.auth.client.SocialUserInfo
import com.snurdy.backend.api.auth.dto.response.TokenResponse
import com.snurdy.backend.domain.auth.entity.RefreshToken
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.User
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.security.jwt.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var refreshTokenRepository: RefreshTokenRepository
    @Mock lateinit var jwtTokenProvider: JwtTokenProvider
    @Mock lateinit var kakaoSocialProvider: SocialProvider

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(
            listOf(kakaoSocialProvider),
            userRepository,
            refreshTokenRepository,
            jwtTokenProvider
        )
    }

    @Test
    @DisplayName("소셜 로그인 시 기존 유저라면 토큰을 반환하고 리프레시 토큰을 갱신한다")
    fun `social login success with existing user`() {
        // Given
        val authCode = "valid_auth_code"
        val kakaoAccessToken = "kakao_access_token"
        val userId = UUID.randomUUID()
        val socialUserInfo = SocialUserInfo(socialId = "12345", nickname = "너디", email = "test@test.com")
        val existingUser = User(id = userId, socialId = "12345", socialType = SocialType.KAKAO, nickname = "너디")
        val tokenResponse = TokenResponse("access", "refresh", 3600L, 2592000L)

        `when`(kakaoSocialProvider.getSocialType()).thenReturn(SocialType.KAKAO)
        `when`(kakaoSocialProvider.getAccessToken(authCode)).thenReturn(kakaoAccessToken)
        `when`(kakaoSocialProvider.getSocialUserInfo(kakaoAccessToken)).thenReturn(socialUserInfo)
        `when`(userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, "12345")).thenReturn(existingUser)
        `when`(jwtTokenProvider.createToken(userId)).thenReturn(tokenResponse)

        // When
        val result = authService.socialLogin(SocialType.KAKAO, authCode)

        assertAll(
            { assertThat(result.accessToken).isEqualTo("access") },
            { assertThat(result.refreshToken).isEqualTo("refresh") },
            { verify(refreshTokenRepository).deleteByUserId(userId) },
            { verify(refreshTokenRepository).save(any(RefreshToken::class.java)) }
        )
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 액세스 토큰 갱신 시 새 토큰 세트를 반환한다")
    fun `refresh access token success`() {
        // Given
        val refreshTokenStr = "valid_refresh_token"
        val userId = UUID.randomUUID()
        val storedToken = RefreshToken(
            userId = userId,
            token = refreshTokenStr,
            expiryDate = LocalDateTime.now().plusDays(1)
        )
        val newTokenResponse = TokenResponse("new_access", "new_refresh", 3600L, 2592000L)

        `when`(refreshTokenRepository.findByToken(refreshTokenStr)).thenReturn(storedToken)
        `when`(jwtTokenProvider.createToken(userId)).thenReturn(newTokenResponse)

        // When
        val result = authService.refreshAccessToken(refreshTokenStr)

        // Then
        assertThat(result.accessToken).isEqualTo("new_access")
        verify(refreshTokenRepository).deleteByUserId(userId)
        verify(refreshTokenRepository).save(any(RefreshToken::class.java))
    }
}