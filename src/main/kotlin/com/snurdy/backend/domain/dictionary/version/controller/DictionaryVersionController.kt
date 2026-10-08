package com.snurdy.backend.domain.dictionary.version.controller

import com.snurdy.backend.domain.dictionary.version.dto.DictionaryVersionResponse
import com.snurdy.backend.domain.dictionary.version.service.DictionaryVersionService
import com.snurdy.backend.global.constants.DictionarySuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class DictionaryVersionController(
    private val dictionaryVersionService: DictionaryVersionService
) : DictionaryVersionApi {

    override fun getDictionaryVersion(): ApiResponse<DictionaryVersionResponse> =
        ApiResponse.success(DictionarySuccess.GET_DICTIONARY_VERSION, dictionaryVersionService.getDictionaryVersion())
}