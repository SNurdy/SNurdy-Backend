package com.snurdy.backend.internal.auth.controller

import com.snurdy.backend.domain.auth.dto.TokenResponse
import com.snurdy.backend.global.constants.AuthSuccess
import com.snurdy.backend.global.constants.CommonSuccess
import com.snurdy.backend.global.dto.response.ApiResponse
import com.snurdy.backend.internal.auth.service.InternalDevAuthService
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Hidden
@Profile("local", "dev")
@RestController
@RequestMapping("/internal/auth")
class InternalDevAuthController(
    private val internalDevAuthService: InternalDevAuthService,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/dev-token")
    fun issueDevToken(): ApiResponse<TokenResponse> =
        ApiResponse.success(AuthSuccess.LOGIN, internalDevAuthService.issueDevToken())
}