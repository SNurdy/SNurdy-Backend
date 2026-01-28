package com.snurdy.backend.global.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthCheckController {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@GetMapping("/v1/health")
	public ResponseEntity<Map<String, Object>> healthCheck() {
		Map<String, Object> health = new HashMap<>();
		health.put("status", "UP");
		health.put("timestamp", LocalDateTime.now().format(FORMATTER));

		return ResponseEntity.ok(health);
	}
}