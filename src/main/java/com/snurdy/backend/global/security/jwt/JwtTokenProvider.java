package com.snurdy.backend.global.security.jwt;

import com.snurdy.backend.domain.auth.dto.response.TokenResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private final JwtProperties jwtProperties;

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
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.accessTokenExpiration());

		return Jwts.builder()
			.subject(userId.toString())
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(getSecretKey())
			.compact();
	}

	public String generateRefreshToken(UUID userId) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + jwtProperties.refreshTokenExpiration());

		return Jwts.builder()
			.subject(userId.toString())
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(getSecretKey())
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(getSecretKey())
				.build()
				.parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public UUID getUserIdFromToken(String token) {
		String subject = Jwts.parser()
			.verifyWith(getSecretKey())
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getSubject();

		return UUID.fromString(subject);
	}

	private SecretKey getSecretKey() {
		return Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes());
	}
}