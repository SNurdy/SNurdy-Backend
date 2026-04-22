package com.snurdy.backend.domain.user.service

import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.auth.service.AccessTokenBlacklistService
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.UserEntity
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.constants.AuthError
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import com.snurdy.backend.global.jwt.JwtTokenProvider
import com.snurdy.backend.global.jwt.TokenType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class UserAccountServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var accessTokenBlacklistService: AccessTokenBlacklistService
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private lateinit var service: UserAccountService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        refreshTokenRepository = mockk(relaxed = true)
        accessTokenBlacklistService = mockk(relaxed = true)
        jwtTokenProvider = mockk()

        service = UserAccountService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            accessTokenBlacklistService = accessTokenBlacklistService,
            jwtTokenProvider = jwtTokenProvider
        )
    }

    @Test
    fun `withdraw - 토큰의 subject가 호출자 userId와 다르면 TOKEN_OWNER_MISMATCH`() {
        val user = activeUser(USER_ID)
        every { userRepository.findById(USER_ID) } returns Optional.of(user)
        every { jwtTokenProvider.validateToken(ACCESS_TOKEN) } returns true
        every { jwtTokenProvider.getTokenType(ACCESS_TOKEN) } returns TokenType.ACCESS
        every { jwtTokenProvider.getUserIdFromToken(ACCESS_TOKEN) } returns OTHER_USER_ID

        val ex = assertThrows(ApiException::class.java) {
            service.withdraw(USER_ID, ACCESS_TOKEN)
        }

        assertEquals(AuthError.TOKEN_OWNER_MISMATCH, ex.error)
        verify(exactly = 0) { refreshTokenRepository.deleteByUserId(any()) }
        verify(exactly = 0) { accessTokenBlacklistService.blacklistAccessToken(any()) }
    }

    @Test
    fun `withdraw - 토큰 타입이 ACCESS가 아니면 INVALID_TOKEN`() {
        val user = activeUser(USER_ID)
        every { userRepository.findById(USER_ID) } returns Optional.of(user)
        every { jwtTokenProvider.validateToken(ACCESS_TOKEN) } returns true
        every { jwtTokenProvider.getTokenType(ACCESS_TOKEN) } returns TokenType.REFRESH

        val ex = assertThrows(ApiException::class.java) {
            service.withdraw(USER_ID, ACCESS_TOKEN)
        }

        assertEquals(AuthError.INVALID_TOKEN, ex.error)
    }

    @Test
    fun `withdraw - 토큰 owner가 일치하면 soft delete + refresh 삭제 + 블랙리스트`() {
        val user = activeUser(USER_ID)
        every { userRepository.findById(USER_ID) } returns Optional.of(user)
        every { jwtTokenProvider.validateToken(ACCESS_TOKEN) } returns true
        every { jwtTokenProvider.getTokenType(ACCESS_TOKEN) } returns TokenType.ACCESS
        every { jwtTokenProvider.getUserIdFromToken(ACCESS_TOKEN) } returns USER_ID
        every { refreshTokenRepository.deleteByUserId(USER_ID) } just runs

        service.withdraw(USER_ID, ACCESS_TOKEN)

        assertEquals(true, user.isDeleted())
        verify(exactly = 1) { refreshTokenRepository.deleteByUserId(USER_ID) }
        verify(exactly = 1) { accessTokenBlacklistService.blacklistAccessToken(ACCESS_TOKEN) }
    }

    @Test
    fun `withdraw - 사용자 미존재면 NOT_FOUND`() {
        every { userRepository.findById(USER_ID) } returns Optional.empty()

        val ex = assertThrows(ApiException::class.java) {
            service.withdraw(USER_ID, ACCESS_TOKEN)
        }
        assertEquals(UserError.NOT_FOUND, ex.error)
    }

    private fun activeUser(id: UUID): UserEntity = UserEntity(
        id = id,
        socialId = "social-$id",
        socialType = SocialType.KAKAO
    )

    companion object {
        private val USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val OTHER_USER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000099")
        private const val ACCESS_TOKEN = "access-token"
    }
}
