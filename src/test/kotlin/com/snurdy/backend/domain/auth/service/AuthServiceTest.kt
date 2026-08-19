package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.auth.dto.UserInfo
import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.domain.auth.external.provider.AppleSocialProvider
import com.snurdy.backend.domain.auth.external.provider.GoogleSocialProvider
import com.snurdy.backend.domain.auth.external.provider.KakaoSocialProvider
import com.snurdy.backend.domain.auth.model.RefreshToken
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.jwt.JwtProperties
import com.snurdy.backend.global.jwt.JwtTokenProvider
import com.snurdy.backend.global.jwt.TokenPair
import com.snurdy.backend.global.jwt.TokenType
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
import java.time.Instant
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    private lateinit var kakaoSocialProvider: KakaoSocialProvider
    private lateinit var appleSocialProvider: AppleSocialProvider
    private lateinit var googleSocialProvider: GoogleSocialProvider
    private lateinit var userRepository: UserRepository
    private lateinit var userRegistrationService: UserRegistrationService
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var accessTokenBlacklistService: AccessTokenBlacklistService
    private lateinit var jwtProperties: JwtProperties

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        kakaoSocialProvider = mockk()
        appleSocialProvider = mockk()
        googleSocialProvider = mockk()
        userRepository = mockk()
        userRegistrationService = mockk()
        refreshTokenRepository = mockk()
        jwtTokenProvider = mockk()
        every { jwtTokenProvider.getTokenType(any()) } returns TokenType.REFRESH
        accessTokenBlacklistService = mockk(relaxed = true)
        jwtProperties = JwtProperties(
            secretKey = "test-secret-test-secret-test-secret-test-secret",
            accessTokenExpiration = ACCESS_TOKEN_EXPIRATION,
            refreshTokenExpiration = REFRESH_TOKEN_EXPIRATION
        )

        authService = AuthService(
            kakaoSocialProvider = kakaoSocialProvider,
            appleSocialProvider = appleSocialProvider,
            googleSocialProvider = googleSocialProvider,
            userRepository = userRepository,
            userRegistrationService = userRegistrationService,
            refreshTokenRepository = refreshTokenRepository,
            jwtTokenProvider = jwtTokenProvider,
            accessTokenBlacklistService = accessTokenBlacklistService,
            jwtProperties = jwtProperties
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
        every { userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNull(SocialType.KAKAO, SOCIAL_ID) } returns existingUser
        every { jwtTokenProvider.createTokenPair(existingUser.id, existingUser.role.name) } returns createTokenPair()
        every { refreshTokenRepository.save(capture(savedRefreshToken)) } just runs

        // When
        val result = authService.kakaoLogin(ACCESS_TOKEN)

        // Then
        assertEquals(issuedToken, result)
        assertEquals(existingUser.id, savedRefreshToken.captured.userId)
        assertEquals(issuedToken.refreshToken, savedRefreshToken.captured.token)
        assertEquals(THIRTY_DAYS_IN_SECONDS, savedRefreshToken.captured.ttlSeconds)

        verify(exactly = 1) { userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNull(SocialType.KAKAO, SOCIAL_ID) }
        verify(exactly = 0) { userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNotNull(any(), any()) }
        verify(exactly = 0) { userRegistrationService.registerNewUser(any(), any()) }
    }

    @Test
    fun `카카오 로그인 중 동시성으로 저장이 실패하면 재조회한 사용자 기준으로 토큰을 발급한다`() {
        // Given
        val socialUserInfo = createSocialUserInfo()
        val reloadedUser = createUser(id = RELOADED_USER_ID)
        val issuedToken = createTokenResponse(reloadedUser.id)

        every { kakaoSocialProvider.getUserInfo(ACCESS_TOKEN) } returns socialUserInfo
        every { userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNull(SocialType.KAKAO, SOCIAL_ID) } returnsMany listOf(null, reloadedUser)
        every { userRegistrationService.registerNewUser(socialUserInfo, SocialType.KAKAO) } throws DataIntegrityViolationException("duplicated")
        every { jwtTokenProvider.createTokenPair(reloadedUser.id, reloadedUser.role.name) } returns createTokenPair()
        every { refreshTokenRepository.save(RefreshToken.create(reloadedUser.id, NEW_REFRESH_TOKEN, REFRESH_TOKEN_VALIDITY_DAYS)) } just runs

        // When
        val result = authService.kakaoLogin(ACCESS_TOKEN)

        // Then
        assertEquals(issuedToken, result)
        verify(exactly = 1) { userRegistrationService.registerNewUser(socialUserInfo, SocialType.KAKAO) }
        verify(exactly = 2) { userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNull(SocialType.KAKAO, SOCIAL_ID) }
        verify(exactly = 0) { userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNotNull(any(), any()) }
        verify(exactly = 1) { jwtTokenProvider.createTokenPair(reloadedUser.id, reloadedUser.role.name) }
    }

    @Test
    fun `카카오 로그인 시 탈퇴 이력이 있어도 복구 제안 없이 새 계정을 생성한다`() {
        // Given
        val socialUserInfo = createSocialUserInfo()
        val recreatedUser = createUser(id = RECREATED_USER_ID)
        val issuedToken = createTokenResponse(recreatedUser.id)
        val savedRefreshToken = slot<RefreshToken>()

        every { kakaoSocialProvider.getUserInfo(ACCESS_TOKEN) } returns socialUserInfo
        every { userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNull(SocialType.KAKAO, SOCIAL_ID) } returns null
        every { userRegistrationService.registerNewUser(socialUserInfo, SocialType.KAKAO) } returns recreatedUser
        every { jwtTokenProvider.createTokenPair(recreatedUser.id, recreatedUser.role.name) } returns createTokenPair()
        every { refreshTokenRepository.save(capture(savedRefreshToken)) } just runs

        // When
        val result = authService.kakaoLogin(ACCESS_TOKEN)

        // Then
        assertEquals(issuedToken, result)
        assertEquals(recreatedUser.id, savedRefreshToken.captured.userId)
        verify(exactly = 0) { userRepository.findBySocialTypeAndSocialIdAndDeletedAtIsNotNull(any(), any()) }
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
        every { jwtTokenProvider.createTokenPair(user.id, user.role.name) } returns createTokenPair()
        every { refreshTokenRepository.save(capture(savedRefreshToken)) } just runs

        // When
        val result = authService.reissue(REFRESH_TOKEN)

        // Then
        assertEquals(reissuedToken, result)
        assertEquals(user.id, savedRefreshToken.captured.userId)
        assertEquals(reissuedToken.refreshToken, savedRefreshToken.captured.token)
        assertEquals(THIRTY_DAYS_IN_SECONDS, savedRefreshToken.captured.ttlSeconds)
    }

    @Test
    fun `reissue 시 refresh 가 아닌 토큰이 들어오면 INVALID_TOKEN 예외가 발생하고 저장된 토큰을 유지한다`() {
        // Given
        every { jwtTokenProvider.validateToken(NEW_ACCESS_TOKEN) } returns true
        every { jwtTokenProvider.getTokenType(NEW_ACCESS_TOKEN) } returns TokenType.ACCESS

        // When
        val exception = assertThrows(ApiException::class.java) {
            authService.reissue(NEW_ACCESS_TOKEN)
        }

        // Then
        assertEquals(AuthError.INVALID_TOKEN, exception.error)
        verify(exactly = 0) { refreshTokenRepository.findByUserId(any()) }
        verify(exactly = 0) { refreshTokenRepository.deleteByUserId(any()) }
    }

    @Test
    fun `logout 시 refresh 가 아닌 토큰이 들어오면 INVALID_TOKEN 예외가 발생하고 저장된 토큰을 유지한다`() {
        // Given
        every { jwtTokenProvider.validateToken(NEW_ACCESS_TOKEN) } returns true
        every { jwtTokenProvider.getTokenType(NEW_ACCESS_TOKEN) } returns TokenType.ACCESS

        // When
        val exception = assertThrows(ApiException::class.java) {
            authService.logout(NEW_ACCESS_TOKEN)
        }

        // Then
        assertEquals(AuthError.INVALID_TOKEN, exception.error)
        verify(exactly = 0) { refreshTokenRepository.deleteByUserId(any()) }
    }

    private fun createSocialUserInfo(): SocialUserInfo = SocialUserInfo(
        socialId = SOCIAL_ID,
        email = EMAIL,
        name = NAME,
        nickname = NICKNAME
    )

    private fun createUser(id: UUID = USER_ID, deletedAt: Instant? = null): UserEntity = UserEntity(
        id = id,
        socialId = SOCIAL_ID,
        socialType = SocialType.KAKAO,
        email = EMAIL,
        name = NAME,
        nickname = NICKNAME,
        deletedAt = deletedAt
    )

    private fun createTokenPair(): TokenPair = TokenPair(
        accessToken = NEW_ACCESS_TOKEN,
        refreshToken = NEW_REFRESH_TOKEN,
        accessTokenExpiresIn = ACCESS_TOKEN_EXPIRATION,
        refreshTokenExpiresIn = REFRESH_TOKEN_EXPIRATION
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
            socialType = SocialType.KAKAO
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
        private val RECREATED_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000033")
    }
}
