package com.snurdy.backend.global.security.jwt;

import com.snurdy.backend.domain.auth.dto.response.TokenResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private final JwtProperties jwtProperties;
	private SecretKey secretKey;

	@PostConstruct
	public void init() {
		byte[] keyBytes = jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	public TokenResponse createToken(UUID userId) {
		String accessToken = generateAccessToken(userId);
		String refreshToken = generateRefreshToken(userId);

		return new TokenResponse(
			accessToken,
			refreshToken,
			jwtProperties.accessTokenExpiration(),
			jwtProperties.refreshTokenExpiration()
		);
	}

	public String generateAccessToken(UUID userId) {
		return buildToken(userId, jwtProperties.accessTokenExpiration());
	}

	public String generateRefreshToken(UUID userId) {
		return buildToken(userId, jwtProperties.refreshTokenExpiration());
	}

	private String buildToken(UUID userId, long expirationMillis) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expirationMillis);

		return Jwts.builder()
		.subject(userId.toString())
		.issuedAt(now)
		.expiration(expiryDate)
		.signWith(secretKey)
		.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public UUID getUserIdFromToken(String token) {
		String subject = Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getSubject();

		return UUID.fromString(subject);
	}
}