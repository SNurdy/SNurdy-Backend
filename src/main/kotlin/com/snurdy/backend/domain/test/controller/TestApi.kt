package com.snurdy.backend.domain.test.controller

import com.snurdy.backend.domain.test.dto.StartTestRequest
import com.snurdy.backend.domain.test.dto.StartTestResponse
import com.snurdy.backend.global.docs.ApiErrorCodes
import com.snurdy.backend.global.docs.ApiSuccessCode
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.security.CurrentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

@Tag(name = "Test", description = "v2 테스트 API")
@RequestMapping("/v2/test")
interface TestApi {

    @Operation(summary = "테스트 시작 (문제 출제)")
    @ApiSuccessCode("LEARNING_S020")
    @ApiErrorCodes("DICTIONARY_E001", "LEARNING_E020")
    @PostMapping("/start")
    fun startTest(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: StartTestRequest
    ): ApiResponse<StartTestResponse>
}
