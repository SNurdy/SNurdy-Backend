package com.snurdy.backend.domain.user.customNote.service

import com.snurdy.backend.domain.user.customNote.repository.CustomNoteShareCodeRepository
import com.snurdy.backend.global.constants.CustomNoteError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.stereotype.Component
import java.security.SecureRandom

// 8자 공유 코드 발급기
// 모호 문자 (0, O, I, l, 1) 제외해 손 입력 가능. UNIQUE 충돌 시 재시도 최대 MAX_RETRY 회
@Component
class ShareCodeGenerator(
    private val shareCodeRepository: CustomNoteShareCodeRepository
) {

    private val random = SecureRandom()

    fun generateUnique(): String {
        repeat(MAX_RETRY) {
            val candidate = generateCandidate()
            if (!shareCodeRepository.existsByShareCode(candidate)) {
                return candidate
            }
        }
        throw ApiException(CustomNoteError.SHARE_CODE_GENERATION_FAILED)
    }

    private fun generateCandidate(): String =
        buildString(CODE_LENGTH) {
            repeat(CODE_LENGTH) {
                append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
        }

    companion object {
        private const val CODE_LENGTH = 8
        private const val MAX_RETRY = 3

        // 모호 문자 제외 (0, O, I, l, 1). 총 54자
        private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz"
    }
}
