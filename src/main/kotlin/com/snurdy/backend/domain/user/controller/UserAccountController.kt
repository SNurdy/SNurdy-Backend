package com.snurdy.backend.domain.user.controller

import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.domain.user.service.UserAccountService
import com.snurdy.backend.global.constants.AuthSuccess
import com.snurdy.backend.global.constants.UserSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.global.extension.currentUserId
import com.snurdy.backend.global.extension.extractBearerToken
import org.springframework.web.bind.annotation.RestController

@RestController
class UserAccountController(
    private val userAccountService: UserAccountService
) : UserAccountApi {

    override fun withdraw(accessToken: String): ApiResponse<Unit> {
        userAccountService.withdraw(
            userId = currentUserId(),
            accessToken = accessToken.extractBearerToken()
        )
        return ApiResponse.success(UserSuccess.WITHDRAW)
    }

    override fun recover(): ApiResponse<TokenResponse> =
        ApiResponse.success(AuthSuccess.LOGIN, userAccountService.recover(currentUserId()))
}
