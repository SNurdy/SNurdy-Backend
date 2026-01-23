package com.snurdy.backend.global.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
	@GetMapping("/")
	public String root() {
		return "SNurdy API Server Running";
	}
	@GetMapping("/api/v1/health")
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("UP");
	}
}
