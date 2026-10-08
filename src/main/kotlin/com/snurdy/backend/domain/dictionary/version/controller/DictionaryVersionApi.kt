package com.snurdy.backend.domain.dictionary.version.controller

import com.snurdy.backend.domain.dictionary.version.dto.DictionaryVersionResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Dictionary", description = "사전 API")
@RequestMapping("/v1/dictionary")
interface DictionaryVersionApi {

    @Operation(
        summary = "사전 버전 조회",
        description = "사전 데이터가 바뀌면 버전 값이 바뀐다. 클라이언트는 저장된 버전과 다를 때만 전체 동기화한다"
    )
    @ApiSuccessCode("DICTIONARY_S009")
    @ApiErrorCodes
    @GetMapping("/version")
    fun getDictionaryVersion(): ApiResponse<DictionaryVersionResponse>
}