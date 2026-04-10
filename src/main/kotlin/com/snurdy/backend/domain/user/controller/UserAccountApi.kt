package com.snurdy.backend.domain.user.controller

import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "User Account", description = "계정 관리 API")
@RequestMapping("/v1/users/account")
interface UserAccountApi {

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping
    fun withdraw(
        @RequestHeader("Authorization") accessToken: String
    ): ApiResponse<Unit>
}
