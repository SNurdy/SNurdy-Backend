package com.snurdy.backend.domain.dictionary.version.service

import com.snurdy.backend.domain.dictionary.version.dto.DictionaryVersionResponse
import com.snurdy.backend.domain.dictionary.version.repository.DictionaryVersionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DictionaryVersionService(
    private val dictionaryVersionRepository: DictionaryVersionRepository
) {

    @Transactional(readOnly = true)
    fun getDictionaryVersion(): DictionaryVersionResponse =
        DictionaryVersionResponse(version = dictionaryVersionRepository.findDictionaryVersion())
}