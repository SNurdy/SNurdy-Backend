package com.snurdy.backend.api.health

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthCheckController {
    @GetMapping("/")
    fun root() = "SNurdy API Server Running"

    @GetMapping("/v1/health")
    fun healthCheck(): ResponseEntity<String?> {
        return ResponseEntity.ok<String?>("UP")
    }
}