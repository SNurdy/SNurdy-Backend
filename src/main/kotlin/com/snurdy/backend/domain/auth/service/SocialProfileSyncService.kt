package com.snurdy.backend.domain.auth.service

import com.snurdy.backend.domain.user.account.repository.UserRepository
import com.snurdy.backend.global.extension.normalizeToNull
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// 초기 가입 시 이메일을 못 받은 유저를 재로그인 시점에 채우기 위한 짧은 트랜잭션 경계
// processLogin 은 트랜잭션 밖이라 기존 유저 수정 반영을 여기서 담당
@Service
class SocialProfileSyncService(
    private val userRepository: UserRepository
) {

    @Transactional
    fun syncMissingEmail(userId: UUID, email: String?) {
        val normalized = email.normalizeToNull() ?: return
        userRepository.findByIdOrNull(userId)?.backfillEmailIfMissing(normalized)
    }
}
