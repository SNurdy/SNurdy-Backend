package com.snurdy.backend.domain.dictionary.version.controller

import com.snurdy.backend.domain.dictionary.version.docs.DictionaryVersionSwaggerDescription
import com.snurdy.backend.domain.dictionary.version.dto.DictionaryVersionResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Tag(
    name = DictionaryVersionSwaggerDescription.DICTIONARY_VERSION_API_TAG_NAME,
    description = DictionaryVersionSwaggerDescription.DICTIONARY_VERSION_API_TAG_DESCRIPTION
)
@RequestMapping("/v1/dictionary")
interface DictionaryVersionApi {

    @Operation(
        summary = DictionaryVersionSwaggerDescription.GET_DICTIONARY_VERSION_SUMMARY,
        description = DictionaryVersionSwaggerDescription.GET_DICTIONARY_VERSION_DESCRIPTION
    )
    @ApiSuccessCode("DICTIONARY_S009")
    @ApiErrorCodes
    @GetMapping("/version")
    fun getDictionaryVersion(): ApiResponse<DictionaryVersionResponse>
}