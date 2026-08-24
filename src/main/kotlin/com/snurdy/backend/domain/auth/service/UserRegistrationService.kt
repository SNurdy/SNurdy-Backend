package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.domain.auth.external.dto.common.SocialUserInfo
import com.snurdy.backend.domain.auth.external.dto.common.toEntity
import com.snurdy.backend.domain.user.account.entity.SocialType
import com.snurdy.backend.domain.user.account.entity.UserEntity
import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.domain.user.account.service.NicknameGenerator
import com.snurdy.backend.domain.user.preference.entity.UserPreferenceEntity
import com.snurdy.backend.domain.user.preference.repository.UserPreferenceRepository
import com.snurdy.backend.global.extension.normalizeToNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 신규 유저 저장만 담당하는 짧은 트랜잭션 경계
// 소셜 로그인 흐름의 외부 API 호출과 Redis 토큰 저장을 트랜잭션 밖에 두기 위해 분리
@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val nicknameGenerator: NicknameGenerator
) {

    // saveAndFlush 로 INSERT 를 즉시 실행해 중복 가입 unique 충돌이 이 메서드 안에서 터지게 함
    // 경합 복구는 호출자가 unique 제약 예외를 받아 처리
    @Transactional
    fun registerNewUser(userInfo: SocialUserInfo, socialType: SocialType): UserEntity {
        // 소셜 닉네임이 null 이거나 blank 면 랜덤 닉네임을 부여
        val nickname = userInfo.nickname.normalizeToNull() ?: nicknameGenerator.generate()
        val savedUser = userRepository.saveAndFlush(userInfo.toEntity(socialType, nickname))
        userPreferenceRepository.save(UserPreferenceEntity(userId = savedUser.id))
        return savedUser
    }
}
