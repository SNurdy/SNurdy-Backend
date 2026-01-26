package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.api.auth.dto.response.TokenResponse
import com.snurdy.backend.api.auth.client.SocialProvider
import com.snurdy.backend.domain.auth.entity.RefreshToken
import com.snurdy.backend.domain.auth.repository.RefreshTokenRepository
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.domain.user.entity.User
import com.snurdy.backend.domain.user.repository.UserRepository
import com.snurdy.backend.global.security.jwt.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional(readOnly = true)
class AuthService(
    private val socialProviders: List<SocialProvider>,
    private val userRepository: UserRepository, // 패키지 확인
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
) {

    @Transactional
    fun socialLogin(socialType: SocialType, authorizationCode: String): TokenResponse {
        val provider = socialProviders.find { it.getSocialType() == socialType }
            ?: throw IllegalArgumentException("Unsupported social type")

        val accessToken = provider.getAccessToken(authorizationCode)
        val socialUserInfo = provider.getSocialUserInfo(accessToken)

        val user = userRepository.findBySocialTypeAndSocialId(socialType, socialUserInfo.socialId)
            ?: userRepository.save(
                User(
                    id = UUID.randomUUID(), // 신규 가입 시 서버 UUID 생성
                    socialId = socialUserInfo.socialId,
                    socialType = socialType,
                    email = socialUserInfo.email,
                    nickname = socialUserInfo.nickname ?: "Unknown"
                )
            )

        return generateTokens(user.id) // user.id는 UUID
    }

//    @Transactional
//    fun mergeUserAccount(userId: UUID, localUuid: UUID) {
//        val localStudyData = studyDataRepository.findAllByLocalUuid(localUuid)
//        val existingData = studyDataRepository.findAllByUserId(userId)
//
//        // ... 병합 로직 실행 ...
//    }

    private fun generateTokens(userId: UUID): TokenResponse {
        val accessToken = jwtTokenProvider.createAccessToken(userId)
        val refreshToken = jwtTokenProvider.createRefreshToken()

        refreshTokenRepository.deleteByUserId(userId)
        refreshTokenRepository.save(RefreshToken(userId, refreshToken, LocalDateTime.now().plusDays(30)))

        return TokenResponse(accessToken, refreshToken, 3600000, 2592000000)
    }
}