package com.snurdy.backend.domain.user.preference.service

import com.snurdy.backend.domain.dictionary.glossary.repository.GlossaryRepository
import com.snurdy.backend.domain.test.entity.TestType
import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultCardCountResponse
import com.snurdy.backend.domain.user.preference.dto.UpdateDefaultTestTypeResponse
import com.snurdy.backend.domain.user.preference.dto.UpdateSelectedGlossaryResponse
import com.snurdy.backend.domain.user.preference.dto.UserPreferenceResponse
import com.snurdy.backend.domain.user.preference.dto.toDefaultCardCountResponse
import com.snurdy.backend.domain.user.preference.dto.toDefaultTestTypeResponse
import com.snurdy.backend.domain.user.preference.dto.toResponse
import com.snurdy.backend.domain.user.preference.dto.toSelectedGlossaryResponse
import com.snurdy.backend.domain.user.preference.entity.UserPreferenceEntity
import com.snurdy.backend.domain.user.preference.repository.UserPreferenceRepository
import com.snurdy.backend.global.constants.DictionaryError
import com.snurdy.backend.global.constants.UserError
import com.snurdy.backend.global.exception.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserPreferenceService(
    private val userPreferenceRepository: UserPreferenceRepository,
    private val glossaryRepository: GlossaryRepository
) {

    @Transactional(readOnly = true)
    fun getPreference(userId: UUID): UserPreferenceEntity =
        userPreferenceRepository.findByIdOrNull(userId)
            ?: throw ApiException(UserError.ACCOUNT_NOT_FOUND)

    @Transactional(readOnly = true)
    fun getPreferenceResponse(userId: UUID): UserPreferenceResponse =
        getPreference(userId).toResponse()

    @Transactional
    fun updateDefaultTestType(userId: UUID, testType: TestType?): UpdateDefaultTestTypeResponse {
        val preference = getPreference(userId)
        preference.updateDefaultTestType(testType)
        return preference.toDefaultTestTypeResponse()
    }

    @Transactional
    fun updateSelectedGlossary(userId: UUID, glossaryId: Long): UpdateSelectedGlossaryResponse {
        if (!glossaryRepository.existsById(glossaryId)) {
            throw ApiException(DictionaryError.GLOSSARY_NOT_FOUND)
        }
        val preference = getPreference(userId)
        preference.updateSelectedGlossaryId(glossaryId)
        return preference.toSelectedGlossaryResponse()
    }

    @Transactional
    fun updateDefaultCardCount(userId: UUID, count: Int): UpdateDefaultCardCountResponse {
        if (count !in UserPreferenceEntity.ALLOWED_CARD_COUNTS) {
            throw ApiException(UserError.DEFAULT_CARD_COUNT_INVALID)
        }
        val preference = getPreference(userId)
        preference.updateDefaultCardCount(count)
        return preference.toDefaultCardCountResponse()
    }
}
