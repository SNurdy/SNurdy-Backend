package com.snurdy.backend.domain.auth.external.client

import com.snurdy.backend.domain.auth.external.dto.GoogleKeysResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping

@FeignClient(name = "google-auth-client", url = "https://www.googleapis.com/oauth2/v3")
interface GoogleAuthClient {

    @GetMapping("/certs")
    fun getGooglePublicKeys(): GoogleKeysResponse
}