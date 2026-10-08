package com.snurdy.backend.domain.dictionary.version.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사전 버전 조회 응답")
data class DictionaryVersionResponse(
    @field:Schema(
        description = "사전 데이터 버전. 저장된 값과 다르면 전체 동기화가 필요",
        example = "3f9a1c0e8b2d4a71"
    )
    val version: String
)