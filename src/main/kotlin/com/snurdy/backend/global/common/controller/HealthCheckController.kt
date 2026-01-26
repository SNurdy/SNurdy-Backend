package com.snurdy.backend.global.common.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthCheckController {
    @GetMapping("/")
    fun root() = "SNurdy API Server Running"

    @GetMapping("/api/v1/health")
    fun healthCheck(): ResponseEntity<String?> {
        return ResponseEntity.ok<String?>("UP")
    }
}
