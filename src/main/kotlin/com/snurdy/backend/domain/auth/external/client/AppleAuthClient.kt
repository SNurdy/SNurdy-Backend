package com.snurdy.backend.domain.auth.external.client

import com.snurdy.backend.domain.auth.external.dto.AppleKeysResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

@FeignClient(name = "apple-auth-client", url = "https://appleid.apple.com/auth")
interface AppleAuthClient {

    @GetMapping("/keys")
    fun getApplePublicKeys(): AppleKeysResponse
}

