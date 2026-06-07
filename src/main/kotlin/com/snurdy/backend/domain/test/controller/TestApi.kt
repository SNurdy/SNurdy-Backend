package com.snurdy.backend.domain.test.controller

import com.snurdy.backend.domain.test.dto.SubmitTestRequest
import com.snurdy.backend.domain.test.dto.SubmitTestResponse
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

    @Operation(summary = "테스트 결과 일괄 제출")
    @ApiSuccessCode("TEST_S002")
    @ApiErrorCodes("DICTIONARY_E001", "TEST_E002")
    @PostMapping("/submit")
    fun submitTest(
        @CurrentUserId userId: UUID,
        @RequestBody @Valid request: SubmitTestRequest
    ): ApiResponse<SubmitTestResponse>
}
