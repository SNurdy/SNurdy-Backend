package com.snurdy.backend.domain.dictionary.version.dto

import com.snurdy.backend.domain.dictionary.version.docs.DictionaryVersionSwaggerDescription
import com.snurdy.backend.domain.dictionary.version.docs.DictionaryVersionSwaggerExample
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = DictionaryVersionSwaggerDescription.DICTIONARY_VERSION_RESPONSE)
data class DictionaryVersionResponse(
    @field:Schema(
        description = DictionaryVersionSwaggerDescription.DICTIONARY_VERSION_RESPONSE_VERSION,
        example = DictionaryVersionSwaggerExample.DICTIONARY_VERSION_RESPONSE_VERSION
    )
    val version: String
)