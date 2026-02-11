package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.auth.dto.UserInfo
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.domain.auth.external.provider.AppleSocialProvider
import com.snurdy.backend.domain.auth.external.provider.GoogleSocialProvider
import com.snurdy.backend.domain.auth.external.provider.KakaoSocialProvider
import com.snurdy.backend.domain.auth.model.RefreshToken
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.security.jwt.JwtTokenProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    private lateinit var kakaoSocialProvider: KakaoSocialProvider
    private lateinit var appleSocialProvider: AppleSocialProvider
    private lateinit var googleSocialProvider: GoogleSocialProvider
    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        kakaoSocialProvider = mockk()
        appleSocialProvider = mockk()
        googleSocialProvider = mockk()
        userRepository = mockk()
        refreshTokenRepository = mockk()
        jwtTokenProvider = mockk()

        authService = AuthService(
            kakaoSocialProvider = kakaoSocialProvider,
            appleSocialProvider = appleSocialProvider,
            googleSocialProvider = googleSocialProvider,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            jwtTokenProvider = jwtTokenProvider
        )
    }

    @Test
    fun `카카오 로그인 시 기존 사용자가 있으면 신규 생성 없이 토큰을 발급한다`() {
        // Given
        val socialUserInfo = createSocialUserInfo()
        val existingUser = createUser()
        val issuedToken = createTokenResponse(existingUser.id)
        val savedRefreshToken = slot<RefreshToken>()

        every { kakaoSocialProvider.getUserInfo(ACCESS_TOKEN) } returns socialUserInfo
        every { userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, SOCIAL_ID) } returns existingUser
        every { jwtTokenProvider.createToken(existingUser) } returns issuedToken
        every { refreshTokenRepository.save(capture(savedRefreshToken)) } just runs

        // When
        val result = authService.kakaoLogin(ACCESS_TOKEN)

        // Then
        assertEquals(issuedToken, result)
        assertEquals(existingUser.id, savedRefreshToken.captured.userId)
        assertEquals(issuedToken.refreshToken, savedRefreshToken.captured.token)
        assertEquals(THIRTY_DAYS_IN_SECONDS, savedRefreshToken.captured.ttlSeconds)

        verify(exactly = 1) { userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, SOCIAL_ID) }
        verify(exactly = 0) { userRepository.save(existingUser) }
    }

    @Test
    fun `카카오 로그인 중 동시성으로 저장이 실패하면 재조회한 사용자 기준으로 토큰을 발급한다`() {
        // Given
        val socialUserInfo = createSocialUserInfo()
        val reloadedUser = createUser(id = RELOADED_USER_ID)
        val issuedToken = createTokenResponse(reloadedUser.id)
        val savedUserCandidate = slot<UserEntity>()

        every { kakaoSocialProvider.getUserInfo(ACCESS_TOKEN) } returns socialUserInfo
        every { userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, SOCIAL_ID) } returnsMany listOf(null, reloadedUser)
        every { userRepository.save(capture(savedUserCandidate)) } throws DataIntegrityViolationException("duplicated")
        every { jwtTokenProvider.createToken(reloadedUser) } returns issuedToken
        every { refreshTokenRepository.save(RefreshToken.create(reloadedUser.id, NEW_REFRESH_TOKEN, REFRESH_TOKEN_VALIDITY_DAYS)) } just runs

        // When
        val result = authService.kakaoLogin(ACCESS_TOKEN)

        // Then
        assertEquals(issuedToken, result)
        assertEquals(SOCIAL_ID, savedUserCandidate.captured.socialId)
        assertEquals(SocialType.KAKAO, savedUserCandidate.captured.socialType)
        verify(exactly = 2) { userRepository.findBySocialTypeAndSocialId(SocialType.KAKAO, SOCIAL_ID) }
        verify(exactly = 1) { jwtTokenProvider.createToken(reloadedUser) }
    }

    @Test
    fun `reissue 시 리프레시 토큰이 유효하지 않으면 INVALID_TOKEN 예외가 발생한다`() {
        // Given
        every { jwtTokenProvider.validateToken(INVALID_REFRESH_TOKEN) } returns false

        // When
        val exception = assertThrows(ApiException::class.java) {
            authService.reissue(INVALID_REFRESH_TOKEN)
        }

        // Then
        assertEquals(AuthError.INVALID_TOKEN, exception.error)
        verify(exactly = 1) { jwtTokenProvider.validateToken(INVALID_REFRESH_TOKEN) }
        verify(exactly = 0) { jwtTokenProvider.getUserIdFromToken(INVALID_REFRESH_TOKEN) }
    }

    @Test
    fun `reissue 시 저장된 리프레시 토큰이 없으면 EXPIRED_TOKEN 예외가 발생한다`() {
        // Given
        every { jwtTokenProvider.validateToken(REFRESH_TOKEN) } returns true
        every { jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN) } returns USER_ID
        every { refreshTokenRepository.findByUserId(USER_ID) } returns null

        // When
        val exception = assertThrows(ApiException::class.java) {
            authService.reissue(REFRESH_TOKEN)
        }

        // Then
        assertEquals(AuthError.EXPIRED_TOKEN, exception.error)
        verify(exactly = 1) { refreshTokenRepository.findByUserId(USER_ID) }
    }

    @Test
    fun `reissue 시 저장된 리프레시 토큰이 다르면 토큰을 삭제하고 INVALID_TOKEN 예외가 발생한다`() {
        // Given
        every { jwtTokenProvider.validateToken(REFRESH_TOKEN) } returns true
        every { jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN) } returns USER_ID
        every { refreshTokenRepository.findByUserId(USER_ID) } returns STORED_REFRESH_TOKEN
        every { refreshTokenRepository.deleteByUserId(USER_ID) } just runs

        // When
        val exception = assertThrows(ApiException::class.java) {
            authService.reissue(REFRESH_TOKEN)
        }

        // Then
        assertEquals(AuthError.INVALID_TOKEN, exception.error)
        verify(exactly = 1) { refreshTokenRepository.deleteByUserId(USER_ID) }
    }

    @Test
    fun `reissue 시 사용자 조회에 실패하면 USER_NOT_FOUND 예외가 발생한다`() {
        // Given
        every { jwtTokenProvider.validateToken(REFRESH_TOKEN) } returns true
        every { jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN) } returns USER_ID
        every { refreshTokenRepository.findByUserId(USER_ID) } returns REFRESH_TOKEN
        every { userRepository.findById(USER_ID) } returns Optional.empty()

        // When
        val exception = assertThrows(ApiException::class.java) {
            authService.reissue(REFRESH_TOKEN)
        }

        // Then
        assertEquals(UserError.NOT_FOUND, exception.error)
        verify(exactly = 1) { userRepository.findById(USER_ID) }
    }


    @Test
    fun `logout 시 리프레시 토큰이 유효하지 않으면 INVALID_TOKEN 예외가 발생한다`() {
        // Given
        every { jwtTokenProvider.validateToken(INVALID_REFRESH_TOKEN) } returns false

        // When
        val exception = assertThrows(ApiException::class.java) {
            authService.logout(INVALID_REFRESH_TOKEN)
        }

        // Then
        assertEquals(AuthError.INVALID_TOKEN, exception.error)
        verify(exactly = 1) { jwtTokenProvider.validateToken(INVALID_REFRESH_TOKEN) }
        verify(exactly = 0) { refreshTokenRepository.deleteByUserId(USER_ID) }
    }

    @Test
    fun `logout 시 저장된 리프레시 토큰이 없으면 EXPIRED_TOKEN 예외가 발생한다`() {
        // Given
        every { jwtTokenProvider.validateToken(REFRESH_TOKEN) } returns true
        every { jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN) } returns USER_ID
        every { refreshTokenRepository.findByUserId(USER_ID) } returns null

        // When
        val exception = assertThrows(ApiException::class.java) {
            authService.logout(REFRESH_TOKEN)
        }

        // Then
        assertEquals(AuthError.EXPIRED_TOKEN, exception.error)
        verify(exactly = 0) { refreshTokenRepository.deleteByUserId(USER_ID) }
    }


    @Test
    fun `logout 시 저장된 리프레시 토큰과 다르면 삭제 후 INVALID_TOKEN 예외가 발생한다`() {
        // Given
        every { jwtTokenProvider.validateToken(REFRESH_TOKEN) } returns true
        every { jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN) } returns USER_ID
        every { refreshTokenRepository.findByUserId(USER_ID) } returns STORED_REFRESH_TOKEN
        every { refreshTokenRepository.deleteByUserId(USER_ID) } just runs

        // When
        val exception = assertThrows(ApiException::class.java) {
            authService.logout(REFRESH_TOKEN)
        }

        // Then
        assertEquals(AuthError.INVALID_TOKEN, exception.error)
        verify(exactly = 1) { refreshTokenRepository.deleteByUserId(USER_ID) }
    }

    @Test
    fun `logout 시 토큰 검증을 통과하면 저장된 리프레시 토큰을 삭제한다`() {
        // Given
        every { jwtTokenProvider.validateToken(REFRESH_TOKEN) } returns true
        every { jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN) } returns USER_ID
        every { refreshTokenRepository.findByUserId(USER_ID) } returns REFRESH_TOKEN
        every { refreshTokenRepository.deleteByUserId(USER_ID) } just runs

        // When
        authService.logout(REFRESH_TOKEN)

        // Then
        verify(exactly = 1) { refreshTokenRepository.deleteByUserId(USER_ID) }
    }

    @Test
    fun `reissue 시 검증을 통과하면 새로운 토큰을 발급하고 리프레시 토큰을 갱신한다`() {
        // Given
        val user = createUser()
        val reissuedToken = createTokenResponse(user.id)
        val savedRefreshToken = slot<RefreshToken>()

        every { jwtTokenProvider.validateToken(REFRESH_TOKEN) } returns true
        every { jwtTokenProvider.getUserIdFromToken(REFRESH_TOKEN) } returns user.id
        every { refreshTokenRepository.findByUserId(user.id) } returns REFRESH_TOKEN
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { jwtTokenProvider.createToken(user) } returns reissuedToken
        every { refreshTokenRepository.save(capture(savedRefreshToken)) } just runs

        // When
        val result = authService.reissue(REFRESH_TOKEN)

        // Then
        assertEquals(reissuedToken, result)
        assertEquals(user.id, savedRefreshToken.captured.userId)
        assertEquals(reissuedToken.refreshToken, savedRefreshToken.captured.token)
        assertEquals(THIRTY_DAYS_IN_SECONDS, savedRefreshToken.captured.ttlSeconds)
    }

    private fun createSocialUserInfo(): SocialUserInfo = SocialUserInfo(
        socialId = SOCIAL_ID,
        email = EMAIL,
        name = NAME,
        nickname = NICKNAME
    )

    private fun createUser(id: UUID = USER_ID): UserEntity = UserEntity(
        id = id,
        socialId = SOCIAL_ID,
        socialType = SocialType.KAKAO,
        email = EMAIL,
        name = NAME,
        nickname = NICKNAME
    )

    private fun createTokenResponse(userId: UUID): TokenResponse = TokenResponse(
        accessToken = NEW_ACCESS_TOKEN,
        refreshToken = NEW_REFRESH_TOKEN,
        accessTokenExpiresIn = ACCESS_TOKEN_EXPIRATION,
        refreshTokenExpiresIn = REFRESH_TOKEN_EXPIRATION,
        user = UserInfo(
            id = userId,
            email = EMAIL,
            name = NAME,
            nickname = NICKNAME,
            provider = SocialType.KAKAO
        )
    )

    companion object {
        private const val ACCESS_TOKEN = "kakao-access-token"
        private const val REFRESH_TOKEN = "refresh-token"
        private const val INVALID_REFRESH_TOKEN = "invalid-refresh-token"
        private const val STORED_REFRESH_TOKEN = "stored-refresh-token"
        private const val NEW_ACCESS_TOKEN = "new-access-token"
        private const val NEW_REFRESH_TOKEN = "new-refresh-token"
        private const val SOCIAL_ID = "social-user-123"
        private const val EMAIL = "snurdy@example.com"
        private const val NAME = "Snurdy"
        private const val NICKNAME = "snurdy-user"
        private const val ACCESS_TOKEN_EXPIRATION = 3_600_000L
        private const val REFRESH_TOKEN_EXPIRATION = 2_592_000_000L
        private const val REFRESH_TOKEN_VALIDITY_DAYS = 30L
        private const val THIRTY_DAYS_IN_SECONDS = 2_592_000L

        private val USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val RELOADED_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000022")
    }
}
