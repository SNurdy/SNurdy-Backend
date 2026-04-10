package com.snurdy.backend.domain.user.controller

import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.global.dto.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader

@Tag(name = "User Account", description = "계정 관리 API")
@RequestMapping("/v1/users/account")
interface UserAccountApi {

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping
    fun withdraw(
        @RequestHeader("Authorization") accessToken: String
    ): ApiResponse<Unit>

    @Operation(summary = "탈퇴 계정 복구")
    @PostMapping("/recover")
    fun recover(): ApiResponse<TokenResponse>
}
