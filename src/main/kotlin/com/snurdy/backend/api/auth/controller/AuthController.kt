package com.snurdy.backend.api.auth.controller

import com.snurdy.backend.api.auth.dto.request.AccountMergeRequest
import com.snurdy.backend.api.auth.dto.request.KakaoLoginRequest
import com.snurdy.backend.api.auth.dto.response.TokenResponse
import com.snurdy.backend.domain.auth.service.AuthService
import com.snurdy.backend.domain.user.entity.SocialType
import com.snurdy.backend.global.common.constants.SuccessType
import com.snurdy.backend.global.common.dto.response.ApiResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {
    @PostMapping("/kakao")
    fun kakaoLogin(@RequestBody request: KakaoLoginRequest): ApiResponse<TokenResponse> {
        val token = authService.socialLogin(SocialType.KAKAO, request.authorizationCode)
        return ApiResponse.success(SuccessType.LOGIN_SUCCESS, token)
    }

//    @PostMapping("/merge")
//    fun mergeData(
//        @AuthenticationPrincipal userId: UUID,
//        @RequestBody request: AccountMergeRequest
//    ): ApiResponse<Unit> {
//        authService.mergeUserAccount(userId, request.localUuid)
//        return ApiResponse.success(SuccessType.MERGE_SUCCESS)
//    }
}