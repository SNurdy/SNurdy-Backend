package com.snurdy.backend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(nullable = false, columnDefinition = "BINARY(16)")
	private UUID userId;

	@Column(nullable = false, unique = true, length = 500)
	private String token;

	@Column(nullable = false)
	private LocalDateTime expiryDate;

	public static RefreshToken of(UUID userId, String token, int validityDays) {
		return RefreshToken.builder()
			.userId(userId)
			.token(token)
			.expiryDate(LocalDateTime.now().plusDays(validityDays))
			.build();
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(this.expiryDate);
	}
}